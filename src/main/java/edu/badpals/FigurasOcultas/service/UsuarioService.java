package edu.badpals.FigurasOcultas.service;

import edu.badpals.FigurasOcultas.model.dto.UsuarioDTO;
import edu.badpals.FigurasOcultas.model.entity.Usuario;
import edu.badpals.FigurasOcultas.model.repository.UsuarioRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Transactional
    public List<UsuarioDTO> getAllUsersDTO() {
        return StreamSupport.stream(usuarioRepository.findAll().spliterator(), false)
                .map(u -> modelMapper.map(u, UsuarioDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional
    public UsuarioDTO getUserById(Long id) {
        return usuarioRepository.findById(id)
                .map(u -> modelMapper.map(u, UsuarioDTO.class))
                .orElse(null);
    }

    @Transactional
    public Usuario getUserByIdNormal(Long id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    @Transactional
    public UsuarioDTO saveUser(UsuarioDTO usuarioDTO) {
        return modelMapper.map(usuarioRepository.save(modelMapper.map(usuarioDTO, Usuario.class)), UsuarioDTO.class);
    }

    @Transactional
    public void deleteUser(Long id) {
        usuarioRepository.deleteById(id);
    }

    @Transactional
    public UsuarioDTO getUserByEmail(String email) {
        return getAllUsersDTO().stream()
                .filter(u -> u.getEmail().equals(email))
                .findFirst()
                .orElse(null);
    }
}