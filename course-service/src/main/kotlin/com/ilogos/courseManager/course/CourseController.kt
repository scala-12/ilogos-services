package com.ilogos.courseManager.course

import com.ilogos.courseManager.exception.ExceptionWithStatus
import com.ipoint.coursegenerator.core.Parser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path

@RestController
@RequestMapping("/api/course")
class CourseController() {

    @Operation(
        summary = "Transform docx file to SCORM module", description = "", requestBody = RequestBody(
            content = [Content(
                mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = Schema(
                    type = "string", format = "binary", description = "DOCX файл"
                )
            )]
        ), responses = [
            ApiResponse(
                responseCode = "200",
                description = "Zip archive with processed content",
                content = [Content(mediaType = "application/zip")]
            ),
            ApiResponse(responseCode = "400", description = "Bad request"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    @PostMapping("/transformDocx", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun transformDocx(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("maxHeader") maxHeader: Int,
        @RequestParam("courseName") courseName: String
    ): ResponseEntity<Any> {
        if (file.isEmpty) {
            return ResponseEntity.badRequest().build()
        }

        val filename = file.originalFilename ?: ""
        if (!filename.endsWith(".docx", ignoreCase = true)) {
            return ResponseEntity.badRequest().build()
        }

        val tempDir: Path = Files.createTempDirectory("$courseName-")
        val parser = Parser()
        var coursePath: String
        file.inputStream.use {
            coursePath = parser.parse(it, maxHeader, courseName, tempDir.toString())
                ?: throw ExceptionWithStatus(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    null
                )
        }

        val file = tempDir.resolve(coursePath).toFile()
        val resource: Resource = FileSystemResource(file)

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$courseName.zip\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM).contentLength(file.length()).body(resource)
    }
}
