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

    @Autowired
    private HistorialTransaccionesService htsService;

    @Transactional(readOnly = true)
    public List<Insignia> getAllInsignias() {
        return insigniaRepository.findAll();
    }

    @Transactional
    public void saveInsignia(Insignia insignia, Long alumnoId) {

        UsuarioDTO alumno = usuarioService.getUserById(alumnoId);

        insignia.setAlumno(modelMapper.map(alumno, Usuario.class));

        insigniaRepository.save(insignia);

        htsService.newInsignia(alumnoId, insignia.getNombre());

    }

    @Transactional
    public void deleteInsignia(Long id) {
        insigniaRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(value = "insigniaImagen", key = "#id")
    public byte[] obtenerImagenInsignia(Long id) {
        Insignia insignia = insigniaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Insignia no encontrada"));
        if (insignia.getImagen() == null) {
            throw new RuntimeException("La insignia no tiene imagen");
        }
        return insignia.getImagen();
    }

    @Transactional(readOnly = true)
    public List<Insignia> getInsigniasAlumno(Long idAlumno) {
        return insigniaRepository.findByAlumnoId(idAlumno);
    }
}