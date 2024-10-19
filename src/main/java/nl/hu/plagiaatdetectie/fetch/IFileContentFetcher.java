package nl.hu.plagiaatdetectie.fetch;

import java.util.UUID;

public interface IFileContentFetcher {

	String fetchFileContent(UUID fileId) throws FileContentFetchException;
}