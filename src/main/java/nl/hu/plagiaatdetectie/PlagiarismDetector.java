package nl.hu.plagiaatdetectie;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import nl.hu.plagiaatdetectie.fetch.IFileContentFetcher;
import nl.hu.plagiaatdetectie.library.algorithm.DetectorGranularity;
import nl.hu.plagiaatdetectie.library.algorithm.DetectorInput;
import nl.hu.plagiaatdetectie.library.algorithm.DetectorResultData;
import nl.hu.plagiaatdetectie.library.algorithm.DetectorResultEntry;
import nl.hu.plagiaatdetectie.library.algorithm.DetectorScorers;
import nl.hu.plagiaatdetectie.library.algorithm.DetectorStartData;
import nl.hu.plagiaatdetectie.library.algorithm.IDetector;
import nl.hu.plagiaatdetectie.library.rabbitmq.FileCheckingData;
import nl.hu.plagiaatdetectie.utils.Make;

/**
 * Plagiarism detector where the command will be generated and executed.
 */
public final class PlagiarismDetector implements IDetector {

	private final IFileContentFetcher fetcher;

	public PlagiarismDetector(IFileContentFetcher fetcher) {
		this.fetcher = fetcher;
	}

	@Override
	public DetectorResultData start(DetectorStartData startData) throws IOException {
		File inputDir = PlagiarismDetector.createTempDirectory("input");
		for (FileCheckingData fileData : startData.fileData()) {
			File file = new File(inputDir, fileData.fileUuid() + "." + fileData.language().toString().toLowerCase(Locale.ROOT));
			Files.writeString(file.toPath(), this.fetcher.fetchFileContent(fileData.fileUuid()),
					StandardCharsets.UTF_8);
		}

		File outputDir = PlagiarismDetector.createTempDirectory("output");
		File csvFileResults = new File(outputDir, "result.csv");
		File csvFilePercentage = new File(outputDir, "percentage.csv");
		File csvFileStats = new File(outputDir, "stats.csv");

		fett.CloneDetection.main(new DetectorInput()
				.scoring(DetectorScorers.SMITH_WATERMAN)
				.granularity(DetectorGranularity.FILE)
				.scoringOptions("similarity=classBased", "matchScore=1", "gapScore=-2", "classFile=/new.json")
				.directoryMode()
				.recursive()
				.jobs(6)
				.threshold(startData.plagiarismThreshold())
				.thresholdMargin(startData.plagiarismThresholdMargin())
				.csvFilePercentage(csvFilePercentage.getAbsolutePath())
				.csvFileStats(csvFileStats.getAbsolutePath())
				.csvFileResults(csvFileResults.getAbsolutePath())
				.compare(inputDir.getAbsolutePath(), inputDir.getAbsolutePath())
				.build());

		DetectorResultData resultData = null;
		try (CSVReader reader = new CSVReader(new FileReader(csvFilePercentage))) {
			List<String[]> rows = reader.readAll();
			String[] names1 = rows.get(0);

			List<DetectorResultEntry> entries = Make.make(new ArrayList<>(), list -> {
				for (int i = 1; i < rows.size(); ++i) {
					String[] rowElements = rows.get(i);
					String name2 = rowElements[0].split("\\.")[0];
					for (int j = 1; j < rowElements.length - 1; ++j) { // The CSV files have a leading comma for some
						// weird reason which makes this loop go kapoet when we try to read that value, hence the -1.
						String value = rowElements[j];
						if (!"skip".equals(value)) {
							String name1 = names1[j].split("\\.")[0];
							list.add(new DetectorResultEntry(UUID.fromString(name1), UUID.fromString(name2),
									Double.parseDouble(value)));
						}
					}
				}
			});
			resultData = new DetectorResultData(startData.submissionId(),
					entries.stream().mapToDouble(DetectorResultEntry::percentage).average().orElse(0),
					entries.toArray(DetectorResultEntry[]::new));
		} catch (CsvException e) {
			throw new IOException(e);
		}

		Files.walkFileTree(inputDir.getParentFile().toPath(), new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}
		});

		return resultData;
	}

	private static File createTempDirectory(String dirName) {
		return Make.make(new File("/tmp/plagiaat/" + dirName), File::mkdirs);
	}
}