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
    @org.springframework.cache.annotation.Cacheable(value = "webConfig", key = "'name'")
    public String getWebConfig() {
        Optional<Webconfig> config = webConfigRepository.findFirstBy();
        if (config.isPresent()) {
            return config.get().getNombre();
        } else {
            return "Default Web Configuration";
        }
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "webConfig", allEntries = true)
    public void updateWebConfig(String newConfig) {
        Optional<Webconfig> config = webConfigRepository.findFirstBy();
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