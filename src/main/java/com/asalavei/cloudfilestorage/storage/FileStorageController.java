package com.asalavei.cloudfilestorage.storage;

import com.asalavei.cloudfilestorage.security.UserPrincipal;
import com.asalavei.cloudfilestorage.util.HttpUtil;
import com.asalavei.cloudfilestorage.util.PathUtil;
import com.asalavei.cloudfilestorage.validation.constraint.ValidObjectPath;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.asalavei.cloudfilestorage.util.Constants.*;

@Controller
@RequiredArgsConstructor
@Validated
@RequestMapping("/storage")
public class FileStorageController {

    private final FileStorageService fileStorageService;

    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadFile(@RequestParam(PATH_PARAM) @ValidObjectPath String path,
                                                            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        InputStream inputStream = fileStorageService.downloadFile(userPrincipal.getId(), path);
        String fileName = UriUtils.encode(PathUtil.getFileName(path), StandardCharsets.UTF_8);
        String contentDisposition = "attachment; filename*=UTF-8''" + fileName;

        return ResponseEntity.ok()
                .header("Content-Disposition", contentDisposition)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(inputStream));
    }

    @GetMapping("/download-multiple")
    public ResponseEntity<InputStreamResource> downloadFolder(@RequestParam(PATH_PARAM) @ValidObjectPath String path,
                                                              @AuthenticationPrincipal UserPrincipal userPrincipal) {
        InputStream inputStream = fileStorageService.downloadFolderAsZip(userPrincipal.getId(), path);
        String fileName = UriUtils.encode(PathUtil.generateZipFilename(path), StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(inputStream));
    }

    @GetMapping("/search")
    public String search(@RequestParam(QUERY_PARAM) String query, @AuthenticationPrincipal UserPrincipal userPrincipal, Model model) {
        model.addAttribute(OBJECTS_ATTRIBUTE, fileStorageService.search(userPrincipal.getId(), query));
        return SEARCH_VIEW;
    }

    @PostMapping("/upload")
    public String upload(@RequestParam(value = PATH_PARAM, defaultValue = PathUtil.DELIMITER) @ValidObjectPath String path,
                         @RequestParam(FILES_PARAM) List<MultipartFile> files, @AuthenticationPrincipal UserPrincipal userPrincipal,
                         RedirectAttributes redirectAttributes, HttpServletRequest request) {
        files.forEach(file -> fileStorageService.upload(userPrincipal.getId(), file, path));

        redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE, "Upload complete");
        return HttpUtil.redirectToReferer(request);
    }

    @PostMapping("/folders")
    public String createFolder(@Valid ObjectRequestDto objectRequestDto, @AuthenticationPrincipal UserPrincipal userPrincipal,
                               RedirectAttributes redirectAttributes, HttpServletRequest request) {
        fileStorageService.createFolder(userPrincipal.getId(), objectRequestDto.getName(), objectRequestDto.getPath());

        redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE, "Folder created successfully");
        return HttpUtil.redirectToReferer(request);
    }

    @PatchMapping
    public String rename(@Valid ObjectRequestDto objectRequestDto, @AuthenticationPrincipal UserPrincipal userPrincipal,
                         RedirectAttributes redirectAttributes, HttpServletRequest request) {
        fileStorageService.rename(userPrincipal.getId(), objectRequestDto.getName(), objectRequestDto.getPath());

        redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE, "Renamed successfully");
        return HttpUtil.redirectToReferer(request);
    }

    @DeleteMapping
    public String delete(@RequestParam(PATH_PARAM) @ValidObjectPath String path, @AuthenticationPrincipal UserPrincipal userPrincipal,
                         RedirectAttributes redirectAttributes, HttpServletRequest request) {
        fileStorageService.delete(userPrincipal.getId(), path);

        redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE, "Deleted successfully");
        return HttpUtil.redirectToReferer(request);
    }
}
