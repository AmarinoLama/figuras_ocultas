package edu.badpals.FigurasOcultas.service;

import edu.badpals.FigurasOcultas.model.dto.UsuarioDTO;
import edu.badpals.FigurasOcultas.model.entity.*;
import edu.badpals.FigurasOcultas.model.repository.InsigniaRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.StreamSupport;

@Service
public class InsigniaService {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private InsigniaRepository insigniaRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Transactional
    public List<Insignia> getAllInsignias() {
        return StreamSupport.stream(insigniaRepository.findAll().spliterator(), false)
                .toList();
    }

    @Transactional
    public void saveInsignia(Insignia insignia, Long alumnoId) {

        UsuarioDTO alumno = usuarioService.getUserById(alumnoId);

        insignia.setAlumno(modelMapper.map(alumno, Usuario.class));

        insigniaRepository.save(insignia);

    }

    @Transactional
    public void deleteInsignia(Long id) {
        insigniaRepository.deleteById(id);
    }

    public byte[] obtenerImagenInsignia(Long id) {
        Insignia insignia = insigniaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Insignia no encontrada"));
        if (insignia.getImagen() == null) {
            throw new RuntimeException("La insignia no tiene imagen");
        }
        return insignia.getImagen();
    }

    public List<Insignia> getInsigniasAlumno(Long idAlumno) {
        UsuarioDTO alumno = usuarioService.getUserById(idAlumno);
        if (alumno == null) {
            throw new RuntimeException("Alumno no encontrado");
        }
        return insigniaRepository.findByAlumno(modelMapper.map(alumno, Usuario.class));
    }
}