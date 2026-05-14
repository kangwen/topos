package com.topos.web.utils;

import com.alibaba.nacos.common.utils.StringUtils;
import com.topos.common.config.BizConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class AmazonS3Util {
	private S3Client S3CLIENT;
	private S3Presigner s3Presigner;
	@Resource
	private BizConfig bizConfig;
	private static final Pattern S3_URL_PATTERN = Pattern.compile("^https?://([a-zA-Z0-9-]+)\\.s3\\.([a-zA-Z0-9-]+)\\.amazonaws\\.com/(.+)$");
	private static final DateTimeFormatter AWS_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneId.of("UTC"));
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

	@PostConstruct
	private void init() {
		if (bizConfig.getSesClient()) {
			AwsBasicCredentials credentials = AwsBasicCredentials.create(bizConfig.getAwss3AccessKeyId(), bizConfig.getAwss3SecretAccessKey());
			StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);
			S3CLIENT = S3Client.builder()
					.credentialsProvider(credentialsProvider)
					.region(Region.AP_SOUTHEAST_1).build();
			s3Presigner = S3Presigner.builder()
					.credentialsProvider(credentialsProvider)
					.region(Region.AP_SOUTHEAST_1).build();
		} else {
			log.warn("AWS S3Client init was skipped");
		}
	}

	/**
	 * 上传私有文件（默认私有，仅授权用户可访问）
	 *
	 * @param file       待上传文件
	 * @param folderPath S3文件夹路径（如：kyb/documents/）
	 * @return 文件在S3的唯一Key（用于后续下载/删除）
	 */
	@SneakyThrows
	public String uploadPrivateFile(MultipartFile file, String bucketName, String folderPath, boolean generateUniqueFileName) {
		return uploadFile(file, folderPath, bucketName, ObjectCannedACL.PRIVATE, generateUniqueFileName);
	}

	/**
	 * 上传文件
	 *
	 * @param file       待上传文件
	 * @param folderPath S3文件夹路径（如：kyb/documents/）
	 * @return 文件在S3的唯一Key（用于后续下载/删除）
	 */
	@SneakyThrows
	public String uploadPublicFile(MultipartFile file, String bucketName, String folderPath, boolean generateUniqueFileName) {
		return uploadFile(file, folderPath, bucketName, null, generateUniqueFileName);
	}


	/**
	 * 保留文件名右侧指定长度（从左侧截取，留右侧）
	 *
	 * @param fileName        处理后的文件名（已替换非法字符）
	 * @param keepRightLength 保留右侧的字符长度
	 * @return 截断后的文件名
	 */
	private static String keepRightFileName(String fileName, int keepRightLength) {
		if (fileName == null || fileName.isEmpty()) {
			return fileName;
		}

		int lastDotIndex = fileName.lastIndexOf(".");
		String namePart = fileName;
		String extPart = "";
		if (lastDotIndex > 0) {
			namePart = fileName.substring(0, lastDotIndex);
			extPart = fileName.substring(lastDotIndex);
			if (extPart.length() > 20) {
				extPart = extPart.substring(0, 20);
			}
		}
		if (namePart.length() > keepRightLength) {
			namePart = namePart.substring(namePart.length() - keepRightLength);
		}
		return namePart + extPart;
	}

	/**
	 * 通用上传方法
	 *
	 * @param file       待上传文件
	 * @param folderPath S3文件夹路径
	 * @param acl        文件访问权限（PRIVATE/PUBLIC_READ）
	 * @return S3文件Key
	 */
	@SneakyThrows
	private String uploadFile(MultipartFile file, String folderPath, String bucketName, ObjectCannedACL acl, boolean generateUniqueFileName) {
		// 1. 校验文件
		if (file.isEmpty()) {
			throw new IllegalArgumentException("上传文件不能为空");
		}
		// 2. 处理文件名（核心修改逻辑）
		String originalFilename = file.getOriginalFilename();
		if (originalFilename == null || originalFilename.trim().isEmpty()) {
			throw new IllegalArgumentException("文件原始名称不能为空");
		}

		String fileName;
		if (generateUniqueFileName) {
			String dateStr = LocalDate.now().format(DATE_FORMATTER);
			String fileExt = originalFilename.lastIndexOf(".") > 0 ?
					originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
			fileName = dateStr + "/" + UUID.randomUUID() + fileExt;
		} else {
			fileName = originalFilename.replaceAll("[\\\\/:*?\"<>|]", "_");
			fileName = keepRightFileName(fileName, 20);
			String uniqueSubDir = UUID.randomUUID() + "/";
			folderPath = folderPath.endsWith("/") ? folderPath + uniqueSubDir : folderPath + "/" + uniqueSubDir;
		}
		// 3. 构建S3文件Key（文件夹+文件名）
		String s3Key = folderPath.endsWith("/") ? folderPath + fileName : folderPath + "/" + fileName;
		PutObjectRequest.Builder builder = PutObjectRequest.builder()
				.bucket(bucketName)
				.key(s3Key);
		if (StringUtils.hasText(file.getContentType())) {
			builder.contentType(file.getContentType());
		}
		if (acl != null) {
			builder.acl(acl);
		}
		S3CLIENT.putObject(builder.build(), RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
		// 6. 返回S3文件Key
		return s3Key;
	}


	/**
	 * 获取文件访问URL（私有文件返回预签名URL，公有文件返回直接URL）
	 *
	 * @param s3Key         S3文件Key
	 * @param expireSeconds 预签名URL过期时间（私有文件有效，默认3600秒=1小时）
	 * @return 访问URL
	 */
	public String getFileUrl(String s3Key, String bucketName, Integer expireSeconds) {
		int expire = expireSeconds == null ? 3600 : expireSeconds;
		return generateDownloadPresignedUrl(s3Key, bucketName, expire);
	}

	/**
	 * 生成【下载】预签名URL（私有文件下载用，最常用）
	 */
	private String generateDownloadPresignedUrl(String s3Key, String bucketName, Integer expireSeconds) {
		int expire = expireSeconds == null ? 3600 : expireSeconds;

		// 构建下载请求
		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(bucketName)
				.key(s3Key)
				.build();

		// 生成预签名请求
		GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
				.signatureDuration(Duration.ofSeconds(expire))
				.getObjectRequest(getObjectRequest)
				.build();

		// 获取预签名URL
		URL presignedUrl = s3Presigner.presignGetObject(presignRequest).url();
		return presignedUrl.toString();
	}

	/**
	 * 生成【上传】预签名URL（前端直传S3用，可选）
	 */
	private String generateUploadPresignedUrl(String s3Key, Integer expireSeconds) {
		int expire = expireSeconds == null ? 3600 : expireSeconds;

		// 构建上传请求
		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(bizConfig.getBucketName())
				.key(s3Key)
				.build();

		// 生成预签名请求
		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
				.signatureDuration(Duration.ofSeconds(expire))
				.putObjectRequest(putObjectRequest)
				.build();

		// 获取预签名URL
		URL presignedUrl = s3Presigner.presignPutObject(presignRequest).url();
		return presignedUrl.toString();
	}

	@SneakyThrows
	public String uploadFile(MultipartFile file, String bucketName, String name) {
		log.info("---------------- START UPLOAD FILE ----------------");
		log.info("Uploading to bucket '" + bucketName);
		S3CLIENT.putObject(PutObjectRequest.builder()
				.bucket(bucketName).key(name).build(), RequestBody.fromBytes(file.getBytes()));
		log.info("===================== Upload File - Done! =====================");
		return name;
	}

	public void deleteFile(String bucketName, String key) {
		log.info("---------------- START DELETE FILE ----------------");
		log.info("deleteFile to bucket '" + bucketName);
		log.info("deleteFile file,bucketName:{},key:{}", bucketName, key);
		S3CLIENT.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
		log.info("===================== deleteFile File - Done! =====================");
	}

	/**
	 * 判断S3预签名URL是否过期
	 *
	 * @param presignedUrl S3预签名URL
	 * @return true=已过期，false=未过期
	 */
	private boolean isPresignedUrlExpired(String presignedUrl) {
		try {
			// 1. 解析URL参数中的X-Amz-Date和X-Amz-Expires
			String queryStr = presignedUrl.split("\\?")[1];
			String[] params = queryStr.split("&");
			String amzDate = null;
			long expires = 0;

			for (String param : params) {
				String[] keyValue = param.split("=");
				if (keyValue.length != 2) {
					continue;
				}
				String key = keyValue[0];
				String value = keyValue[1];
				if ("X-Amz-Date".equals(key)) {
					amzDate = value;
				} else if ("X-Amz-Expires".equals(key)) {
					expires = Long.parseLong(value);
				}
			}

			// 2. 校验参数是否完整
			if (amzDate == null || expires == 0) {
				log.warn("S3 URL缺少X-Amz-Date/X-Amz-Expires参数，判定为已过期");
				return true;
			}

			// 3. 计算过期时间：签名时间 + 有效期
			Instant signTime = Instant.from(AWS_DATE_FORMATTER.parse(amzDate));
			Instant expireTime = signTime.plusSeconds(expires);
			Instant now = Instant.now();
			boolean isExpired = now.plusSeconds(60).isAfter(expireTime);
			log.info("S3 URL过期校验 - 签名时间：{}，有效期：{}秒，过期时间：{}，当前时间：{}，是否过期：{}",
					signTime, expires, expireTime, now, isExpired);
			return isExpired;

		} catch (Exception e) {
			log.error("判断S3 URL是否过期失败，默认判定为已过期", e);
			return true;
		}
	}

	/**
	 * 核心方法：传入过期URL，生成新预签名URL
	 *
	 * @param expiredPresignedUrl 过期的S3预签名URL
	 * @param expireSeconds       新URL有效期（秒），最大604800（7天）
	 * @return 新的有效预签名URL
	 */
	public String refreshExpiredUrl(String expiredPresignedUrl, int expireSeconds) {
		if (!isPresignedUrlExpired(expiredPresignedUrl)) {
			log.info("S3 URL未过期，直接返回原URL：{}", expiredPresignedUrl);
			return expiredPresignedUrl;
		}
		String bucket;
		String region;
		String s3Key;
		try {
			Matcher matcher = S3_URL_PATTERN.matcher(expiredPresignedUrl);
			if (!matcher.find()) {
				throw new IllegalArgumentException("不支持的S3 URL格式: " + expiredPresignedUrl);
			}

			bucket = matcher.group(1);
			region = matcher.group(2);
			s3Key = matcher.group(3);
			if (s3Key.contains("?")) {
				s3Key = s3Key.split("\\?")[0];
			}
			log.info("Bucket:{},Region:{},S3 Key:{}", bucket, region, s3Key);
		} catch (Exception e) {
			throw new RuntimeException("解析URL失败", e);
		}

		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(bucket)
				.key(s3Key)
				.build();

		GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
				.signatureDuration(Duration.ofSeconds(expireSeconds))
				.getObjectRequest(getObjectRequest)
				.build();

		return s3Presigner.presignGetObject(presignRequest).url().toString();
	}


}
