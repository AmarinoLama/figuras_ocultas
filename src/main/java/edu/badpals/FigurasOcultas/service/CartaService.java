package edu.badpals.FigurasOcultas.service;

import edu.badpals.FigurasOcultas.model.dto.UsuarioDTO;
import edu.badpals.FigurasOcultas.model.entity.Carta;
import edu.badpals.FigurasOcultas.model.repository.CartaRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class CartaService {

    @Autowired
    private CartaRepository cartaRepository;

    @Transactional
    public List<Carta> getAllCartas() {
        return StreamSupport.stream(cartaRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());
    }
}