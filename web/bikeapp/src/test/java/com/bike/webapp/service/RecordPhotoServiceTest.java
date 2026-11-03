package com.bike.webapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import com.bike.webapp.dto.PhotoAccess;
import com.bike.webapp.dto.RidingRecord;
import com.bike.webapp.mapper.AndroidMapper;

class RecordPhotoServiceTest {

	private static final byte[] JPEG = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F'};
	private static final byte[] PNG = new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
	private static final byte[] WEBP = new byte[] {'R', 'I', 'F', 'F', 1, 2, 3, 4, 'W', 'E', 'B', 'P'};

	@TempDir
	Path storage;

	private AndroidMapper mapper;
	private AndroidService androidService;
	private RecordPhotoService service;

	@BeforeEach
	void setUp() {
		mapper = mock(AndroidMapper.class);
		androidService = mock(AndroidService.class);
		when(androidService.getOwnRecord("rider1", 7)).thenReturn(new RidingRecord());
		service = new RecordPhotoService(mapper, androidService, storage.toString(), 1024);
	}

	private MockMultipartFile upload(byte[] bytes) {
		return new MockMultipartFile("photo", "x.jpg", "image/jpeg", bytes);
	}

	private long filesOnDisk() throws IOException {
		Path photos = storage.resolve("photos");
		if (!Files.isDirectory(photos)) {
			return 0;
		}
		try (Stream<Path> files = Files.list(photos)) {
			return files.count();
		}
	}

	@Test
	void ownerCanAddAJpegAndTheFileAndRowAreCreated() throws Exception {
		when(mapper.nextPhotoOrder(7)).thenReturn(0);
		RecordPhotoService.AddResult result = service.add("rider1", 7, upload(JPEG));

		assertEquals(RecordPhotoService.Status.OK, result.status);
		assertTrue(result.photo.getId().matches("[0-9a-f]{32}"));
		ArgumentCaptor<String> fileName = ArgumentCaptor.forClass(String.class);
		verify(mapper).insertPhoto(eq(result.photo.getId()), eq(7), fileName.capture(), eq(0));
		assertTrue(fileName.getValue().endsWith(".jpg"));
		assertEquals(1, filesOnDisk());
	}

	@Test
	void fileTypeComesFromTheContentNotFromTheClientsClaim() {
		assertEquals("jpg", RecordPhotoService.detectExtension(JPEG));
		assertEquals("png", RecordPhotoService.detectExtension(PNG));
		assertEquals("webp", RecordPhotoService.detectExtension(WEBP));
		assertNull(RecordPhotoService.detectExtension("<html>".getBytes()));
		assertNull(RecordPhotoService.detectExtension(new byte[] {1, 2}));
	}

	@Test
	void nonImagesAreRejectedEvenIfNamedJpg() throws Exception {
		RecordPhotoService.AddResult result = service.add("rider1", 7, upload("<?php echo 1;".getBytes()));
		assertEquals(RecordPhotoService.Status.INVALID, result.status);
		assertEquals(0, filesOnDisk());
		verify(mapper, never()).insertPhoto(anyString(), anyInt(), anyString(), anyInt());
	}

	@Test
	void oversizedUploadsAreRejected() throws Exception {
		byte[] big = new byte[2048];
		System.arraycopy(JPEG, 0, big, 0, JPEG.length);
		assertEquals(RecordPhotoService.Status.TOO_LARGE, service.add("rider1", 7, upload(big)).status);
		assertEquals(0, filesOnDisk());
	}

	@Test
	void anotherRidersRecordCannotReceivePhotos() throws Exception {
		assertEquals(RecordPhotoService.Status.NOT_FOUND, service.add("intruder", 7, upload(JPEG)).status);
		verify(mapper, never()).insertPhoto(anyString(), anyInt(), anyString(), anyInt());
		assertEquals(0, filesOnDisk());
	}

	@Test
	void atMostTenPhotosPerRecord() throws Exception {
		when(mapper.countPhotos(7)).thenReturn(RecordPhotoService.MAX_PHOTOS_PER_RECORD);
		assertEquals(RecordPhotoService.Status.TOO_MANY, service.add("rider1", 7, upload(JPEG)).status);
		assertEquals(0, filesOnDisk());
	}

	@Test
	void ifTheRowCannotBeStoredTheFileIsRemovedAgain() throws Exception {
		org.mockito.Mockito.doThrow(new IllegalStateException("db down"))
				.when(mapper).insertPhoto(anyString(), anyInt(), anyString(), anyInt());
		assertEquals(RecordPhotoService.Status.FAILED, service.add("rider1", 7, upload(PNG)).status);
		assertEquals(0, filesOnDisk());
	}

	@Test
	void deleteRemovesRowAndFileOnlyForTheOwner() throws Exception {
		when(mapper.nextPhotoOrder(7)).thenReturn(0);
		String id = service.add("rider1", 7, upload(JPEG)).photo.getId();
		String fileName = storage.resolve("photos").toFile().list()[0];
		when(mapper.findPhotoFile(id, 7)).thenReturn(fileName);
		when(mapper.deletePhoto(id, 7)).thenReturn(1);

		assertFalse(service.delete("intruder", 7, id));
		assertEquals(1, filesOnDisk());
		assertTrue(service.delete("rider1", 7, id));
		assertEquals(0, filesOnDisk());
		assertFalse(service.delete("rider1", 7, "../etc/passwd"));
	}

	@Test
	void privatePhotosAreOnlyServedToTheirOwner() throws Exception {
		when(mapper.nextPhotoOrder(7)).thenReturn(0);
		String id = service.add("rider1", 7, upload(JPEG)).photo.getId();
		String fileName = storage.resolve("photos").toFile().list()[0];

		PhotoAccess access = new PhotoAccess();
		access.setFileName(fileName);
		access.setOwnerId("rider1");
		access.setPublicRecord(false);
		when(mapper.findPhotoAccess(id)).thenReturn(access);

		assertNotNull(service.open(id, "rider1"));
		assertNull(service.open(id, "intruder"));
		assertNull(service.open(id, null));

		access.setPublicRecord(true);
		assertNotNull(service.open(id, null));
		assertEquals("image/jpeg", service.open(id, null).contentType);
	}

	@Test
	void unknownOrMalformedPhotoIdsAreNotFound() {
		assertNull(service.open("../../secret", "rider1"));
		assertNull(service.open("0123456789abcdef0123456789abcdef", "rider1")); // no row
		assertTrue(service.list(7).isEmpty());
	}
}
