package com.dmitriev.i.oleg.si.example.stub.handler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
@RequestMapping("stub")
@Profile("stub")
@RequiredArgsConstructor
public class StubController {
    private final RestStubber restStubber;

    @RequestMapping(path = "**", method = {GET, POST, PUT, DELETE, PATCH})
    public ResponseEntity<Object> stub(HttpServletRequest request) {
        return restStubber.getResponse(request);
    }
}
