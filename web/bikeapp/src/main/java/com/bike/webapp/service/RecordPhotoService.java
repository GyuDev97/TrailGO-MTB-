package com.bike.webapp.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.bike.webapp.dto.PhotoAccess;
import com.bike.webapp.dto.RecordPhoto;
import com.bike.webapp.mapper.AndroidMapper;

/**
 * Photos attached to a riding record.
 *
 * <p>Files live under {@code <storage-root>/photos/<token>.<ext>}; the database only holds the
 * token and the file name (never the image bytes). The file is written first and the row second;
 * if the row cannot be written the file is removed again, so the two never disagree for long. A
 * photo is served to its owner, or to anyone when the record is public. File type is decided from
 * the content, not from the client's file name or content type.</p>
 */
@Service
public class RecordPhotoService {

	public static final int MAX_PHOTOS_PER_RECORD = 10;

	private static final Logger log = LoggerFactory.getLogger(RecordPhotoService.class);

	public enum Status { OK, NOT_FOUND, TOO_MANY, INVALID, TOO_LARGE, FAILED }

	public static final class AddResult {
		public final Status status;
		public final RecordPhoto photo;

		public AddResult(Status status, RecordPhoto photo) {
			this.status = status;
			this.photo = photo;
		}
	}

	/** A photo ready to be streamed. */
	public static final class PhotoFile {
		public final Path path;
		public final String contentType;
		public final boolean publicRecord;

		public PhotoFile(Path path, String contentType, boolean publicRecord) {
			this.path = path;
			this.contentType = contentType;
			this.publicRecord = publicRecord;
		}
	}

	private final AndroidMapper mapper;
	private final AndroidService androidService;
	private final Path photoDirectory;
	private final long maxBytes;

	@Autowired
	public RecordPhotoService(AndroidMapper mapper, AndroidService androidService,
			@Value("${trailgo.storage-root}") String storageRoot,
			@Value("${trailgo.photo.max-bytes:8388608}") long maxBytes) {
		this.mapper = mapper;
		this.androidService = androidService;
		this.photoDirectory = Paths.get(storageRoot).toAbsolutePath().normalize().resolve("photos");
		this.maxBytes = maxBytes;
	}

	public List<RecordPhoto> list(int rrNum) {
		List<RecordPhoto> photos = mapper.listPhotos(rrNum);
		return photos == null ? Collections.<RecordPhoto>emptyList() : photos;
	}

	public AddResult add(String riderId, int rrNum, MultipartFile upload) {
		if (androidService.getOwnRecord(riderId, rrNum) == null) {
			return new AddResult(Status.NOT_FOUND, null);
		}
		if (upload == null || upload.isEmpty()) {
			return new AddResult(Status.INVALID, null);
		}
		if (upload.getSize() > maxBytes) {
			return new AddResult(Status.TOO_LARGE, null);
		}
		if (mapper.countPhotos(rrNum) >= MAX_PHOTOS_PER_RECORD) {
			return new AddResult(Status.TOO_MANY, null);
		}

		byte[] bytes;
		try (InputStream input = upload.getInputStream()) {
			bytes = readAtMost(input, maxBytes);
		} catch (IOException e) {
			log.warn("Photo upload could not be read: {}", e.getClass().getSimpleName());
			return new AddResult(Status.FAILED, null);
		}
		if (bytes == null) {
			return new AddResult(Status.TOO_LARGE, null);
		}
		String extension = detectExtension(bytes);
		if (extension == null) {
			return new AddResult(Status.INVALID, null);
		}

		String id = UUID.randomUUID().toString().replace("-", "");
		String fileName = id + "." + extension;
		Path target = photoDirectory.resolve(fileName);
		Path partial = photoDirectory.resolve(fileName + ".part");
		try {
			Files.createDirectories(photoDirectory);
			Files.write(partial, bytes);
			Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			log.error("Photo file could not be stored: {}", e.getClass().getSimpleName());
			deleteQuietly(partial);
			deleteQuietly(target);
			return new AddResult(Status.FAILED, null);
		}

		int order = mapper.nextPhotoOrder(rrNum);
		try {
			mapper.insertPhoto(id, rrNum, fileName, order);
		} catch (RuntimeException e) {
			log.error("Photo row could not be stored for record {}: {}", rrNum, e.getClass().getSimpleName());
			deleteQuietly(target);
			return new AddResult(Status.FAILED, null);
		}
		RecordPhoto photo = new RecordPhoto();
		photo.setId(id);
		photo.setSortOrder(order);
		return new AddResult(Status.OK, photo);
	}

	/** @return true when the photo existed, belonged to the rider's record and was removed */
	public boolean delete(String riderId, int rrNum, String photoId) {
		if (!isToken(photoId) || androidService.getOwnRecord(riderId, rrNum) == null) {
			return false;
		}
		String fileName = mapper.findPhotoFile(photoId, rrNum);
		if (fileName == null) {
			return false;
		}
		if (mapper.deletePhoto(photoId, rrNum) < 1) {
			return false;
		}
		// The row is gone; a file that cannot be removed is only an orphan, never a visible photo.
		deleteQuietly(photoDirectory.resolve(fileName));
		return true;
	}

	/**
	 * @param requesterId rider asking for the photo (may be null)
	 * @return the file, or null when it does not exist or the requester may not see it
	 */
	public PhotoFile open(String photoId, String requesterId) {
		if (!isToken(photoId)) {
			return null;
		}
		PhotoAccess access = mapper.findPhotoAccess(photoId);
		if (access == null || access.getFileName() == null) {
			return null;
		}
		boolean owner = requesterId != null && requesterId.equals(access.getOwnerId());
		if (!access.isPublicRecord() && !owner) {
			return null;
		}
		Path file = photoDirectory.resolve(access.getFileName()).normalize();
		if (!file.startsWith(photoDirectory) || !Files.isRegularFile(file)) {
			return null;
		}
		return new PhotoFile(file, contentTypeOf(access.getFileName()), access.isPublicRecord());
	}

	static boolean isToken(String value) {
		return value != null && value.matches("[0-9a-f]{32}");
	}

	/** Returns jpg / png / webp from the file signature, or null for anything else. */
	static String detectExtension(byte[] b) {
		if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
			return "jpg";
		}
		if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
				&& b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
			return "png";
		}
		if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
				&& b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
			return "webp";
		}
		return null;
	}

	static String contentTypeOf(String fileName) {
		if (fileName.endsWith(".png")) {
			return "image/png";
		}
		if (fileName.endsWith(".webp")) {
			return "image/webp";
		}
		return "image/jpeg";
	}

	/** @return the bytes, or null when the stream is longer than the limit */
	private static byte[] readAtMost(InputStream input, long limit) throws IOException {
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		byte[] buffer = new byte[16 * 1024];
		long total = 0;
		int read;
		while ((read = input.read(buffer)) != -1) {
			total += read;
			if (total > limit) {
				return null;
			}
			out.write(buffer, 0, read);
		}
		return out.toByteArray();
	}

	private static void deleteQuietly(Path path) {
		try {
			Files.deleteIfExists(path);
		} catch (IOException e) {
			log.warn("Could not delete {}", path.getFileName());
		}
	}
}
