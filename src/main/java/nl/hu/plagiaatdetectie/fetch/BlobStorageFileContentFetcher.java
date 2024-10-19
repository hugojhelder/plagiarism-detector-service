package nl.hu.plagiaatdetectie.fetch;

import java.io.IOException;
import java.util.UUID;
import nl.hu.plagiaatdetectie.Constants;
import nl.hu.plagiaatdetectie.library.BlobStorage;

public class BlobStorageFileContentFetcher implements IFileContentFetcher {

	private final BlobStorage blobClient;

	public BlobStorageFileContentFetcher() throws FileContentFetchException {
		this.blobClient = new BlobStorage(Constants.AZURE_BLOB_KEY, "plagiaatcontrole");
	}

	@Override
	public String fetchFileContent(UUID fileId) throws FileContentFetchException {
		try {
			return this.blobClient.readBlob(fileId);
		} catch (IOException e) {
			throw new FileContentFetchException(e);
		}
	}
}
