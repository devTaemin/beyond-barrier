package com.vd5.beyondb.controller;

import com.vd5.beyondb.model.Program;
import com.vd5.beyondb.model.dto.request.CaptureDto;
import com.vd5.beyondb.model.dto.request.CaptureMlDto;
import com.vd5.beyondb.model.dto.response.CaptionDto;
import com.vd5.beyondb.model.dto.response.DetectDto;
import com.vd5.beyondb.service.ProgramService;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RestController
@RequestMapping("api")
public class ApiController {

    @Autowired
    private ProgramService programService;

    String mlBaseUrl = "http://70.12.130.121:5000/";      // ML server URL
    String crawlingBaseUrl = "https://search.naver.com/search.naver?where=nexearch&sm=top_hty&fbm=0&ie=utf8&query=";        // Naver search URL

    @PostMapping(path="/caption")
    public ResponseEntity<CaptionDto> captionImage(@RequestBody CaptureDto captureDto){
        CaptureMlDto captureMlDto = new CaptureMlDto(captureDto.getImgPath());  // request dto to ML server
        RestTemplate restTemplate = new RestTemplate();
        String result = restTemplate.postForObject(mlBaseUrl + "s3/imagecaption", captureMlDto, String.class);
        CaptionDto captionDto = new CaptionDto(result);
        return new ResponseEntity<>(captionDto, HttpStatus.OK);
    }

    @PostMapping(path = "/program")
    public ResponseEntity<DetectDto> detectLogo(@RequestBody CaptureDto captureDto){
        log.info("===== detectLogo() =====");
        CaptureMlDto captureMlDto = new CaptureMlDto(captureDto.getImgPath());  // request dto to ML server
        RestTemplate restTemplate = new RestTemplate();
        int programId = Integer.parseInt(restTemplate.postForObject(mlBaseUrl + "s3/logodetect", captureMlDto, String.class));
        Program program = programService.findById(programId);       // get program name from DB by program id
        DetectDto detectDto = new DetectDto(program.getId(), program.getName());
        return new ResponseEntity<>(detectDto, HttpStatus.OK);
    }

    @PostMapping(path = "/program/crawling")
    public String crawlingProgram(){
        log.info("===== crawlingProgram() =====");
        Program program = programService.findById(590);
        String programName = program.getName().replace(" ", "+");   // handle spacing
        log.info("프로그램명 : " + programName);
        try {
            Document rawData = Jsoup.connect(crawlingBaseUrl + programName).timeout(5000).get();
            Elements descElem = rawData.select("div.detail_info span.desc._text");
            String desc = descElem.get(0).text();
            log.info("[크롤링] 설명 : " + desc);
            Document rawCastData = Jsoup.connect(crawlingBaseUrl + programName + "+출연진").timeout(5000).get();
            Elements castElem = rawCastData.select("div.item div.title_box strong");
            for(int i = 0; i < castElem.size(); i++){
                String cast = castElem.get(i).text();
                log.info("[크롤링] 출연진 " + (i + 1) + " : " + cast);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return "크롤링 완료";
    }
}
