package com.think123.code.log;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api")
@Slf4j
public class UserController {

    @Autowired
    private RestTemplate restTemplate;



    @PostMapping("/user")
    public String createUser(@RequestBody UserRequest request) {

        log.info("create user");

        return "User[" + request.getName() + "] created successfully";

    }

    @GetMapping("/users/{userId}")
    public Map<String, Object> getUser(@PathVariable("userId") Long userId,@RequestParam Map<String, String> params) {

       Map<String, Object> map = new HashMap<>();

       map.put("userId", userId);
       map.put("name", "think123-" + RandomStringUtils.randomAlphabetic(3));

       if(params.get("age") != null) {
           map.put("age", params.get("age") + RandomUtils.nextInt(1, 20));
       }
       if(params.get("school") != null) {
           map.put("school", params.get("school"));
       }

       return map;
    }

    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam String age, @RequestParam String school) {

        String url = "http://localhost:8080/api/users/1?age="+age+"&school="+school;

        Map<String,Object> map = restTemplate.getForObject(url, Map.class);


        return map;
    }


}
