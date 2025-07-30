package edu.badpals.FigurasOcultas.service;

import edu.badpals.FigurasOcultas.model.entity.*;
import edu.badpals.FigurasOcultas.model.repository.WebConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.StreamSupport;

@Service
public class WebConfigService {

    @Autowired
    private WebConfigRepository webConfigRepository;

    @Transactional
    public String getWebConfig() {
        Optional<Webconfig> config = StreamSupport.stream(webConfigRepository.findAll().spliterator(), false).findFirst();
        if (config.isPresent()) {
            return config.get().getNombre();
        } else {
            return "Default Web Configuration";
        }
    }

    @Transactional
    public void updateWebConfig(String newConfig) {
        Optional<Webconfig> config = StreamSupport.stream(webConfigRepository.findAll().spliterator(), false).findFirst();
        if (config.isPresent()) {
            Webconfig webConfig = config.get();
            webConfig.setNombre(newConfig);
            webConfigRepository.save(webConfig);
        } else {
            Webconfig newWebConfig = new Webconfig();
            newWebConfig.setNombre(newConfig);
            webConfigRepository.save(newWebConfig);
        }
    }
}