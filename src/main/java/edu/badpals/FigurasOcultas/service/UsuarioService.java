package edu.badpals.FigurasOcultas.service;
import edu.badpals.FigurasOcultas.model.dto.UsuarioDTO;
import edu.badpals.FigurasOcultas.model.entity.CursoAlumno;
import edu.badpals.FigurasOcultas.model.entity.RolUsuario;
import edu.badpals.FigurasOcultas.model.entity.TarjetaAlumno;
import edu.badpals.FigurasOcultas.model.entity.Usuario;
import edu.badpals.FigurasOcultas.model.repository.UsuarioRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    @Lazy
    private HistorialTransaccionesService hts;

    @Autowired
    private ModelMapper modelMapper;

    @Transactional
    public List<UsuarioDTO> getAllUsersDTO() {
        return StreamSupport.stream(usuarioRepository.findAll().spliterator(), false)
                .map(u -> modelMapper.map(u, UsuarioDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<UsuarioDTO> getAllAlumnos() {
        return StreamSupport.stream(usuarioRepository.findAll().spliterator(), false)
                .map(u -> modelMapper.map(u, UsuarioDTO.class))
                .filter(u -> !u.isAdmin())
                .collect(Collectors.toList());
    }

    @Transactional
    public List<UsuarioDTO> getAlumnosFromCurso(String curso) {
        CursoAlumno cursoEnum = CursoAlumno.valueOf(curso); // conversión segura
        return StreamSupport.stream(usuarioRepository.findAll().spliterator(), false)
                .map(u -> modelMapper.map(u, UsuarioDTO.class))
                .filter(u -> !u.isAdmin() && u.getCurso() == cursoEnum)
                .collect(Collectors.toList());
    }

    @Transactional
    public UsuarioDTO getUserById(Long id) {
        return usuarioRepository.findById(id)
                .map(u -> modelMapper.map(u, UsuarioDTO.class))
                .orElse(null);
    }

    @Transactional
    public void saveUser(UsuarioDTO usuarioDTO) {
        Usuario usuario = modelMapper.map(usuarioDTO, Usuario.class);
        if (usuarioDTO.getTarjetaAlumno() != null && usuarioDTO.getRol() == RolUsuario.ALUMNO) {
            TarjetaAlumno tarjetaAlumno = modelMapper.map(usuarioDTO.getTarjetaAlumno(), TarjetaAlumno.class);
            tarjetaAlumno.setUsuario(usuario);
            usuario.setTarjetaAlumno(tarjetaAlumno);
            usuarioRepository.save(usuario);
        }
    }

    @Transactional
    public void saveAdmin(Usuario admin) {
        usuarioRepository.save(admin);
    }

    @Transactional
    public boolean checkValidEmail(String email) {
        if (email == null || email.isBlank()) return false;
        return usuarioRepository.findByEmail(email).isEmpty();
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

    @Transactional
    public void darElectroniosCurso(String curso, int cantidadElectronios) {
        List<UsuarioDTO> alumnos = getAlumnosFromCurso(curso);
        for (UsuarioDTO alumno : alumnos) {

            int electronios = alumno.getTarjetaAlumno().getElectronios();

            int nuevos = electronios + cantidadElectronios;

            nuevos = Math.max(-100, Math.min(100, nuevos));

            alumno.getTarjetaAlumno().setElectronios((byte) nuevos);
            System.out.println("Electronios actualizados: " + nuevos);

            if (cantidadElectronios > 0) {
                hts.addMoreElectroniosToHistorial(alumno.getId(), cantidadElectronios);
            } else {
                hts.addLessElectroniosToHistorial(alumno.getId(), -cantidadElectronios);
            }

            saveUser(alumno);
        }
    }

    @Transactional
    public void darElectroniosAlumnos(List<Long> idsAlumnos, int cantidadElectronios) {
        for (Long idAlumno : idsAlumnos) {
            UsuarioDTO alumno = getUserById(idAlumno);
            if (alumno != null) {

                int electronios = alumno.getTarjetaAlumno().getElectronios();

                int nuevos = electronios + cantidadElectronios;

                nuevos = Math.max(-100, Math.min(100, nuevos));

                alumno.getTarjetaAlumno().setElectronios((byte) nuevos);

                if (cantidadElectronios > 0) {
                    hts.addMoreElectroniosToHistorial(alumno.getId(), cantidadElectronios);
                } else {
                    hts.addLessElectroniosToHistorial(alumno.getId(), -cantidadElectronios);
                }

                saveUser(alumno);
            }
        }
    }
}