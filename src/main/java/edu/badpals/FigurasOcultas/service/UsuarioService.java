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
        } else if (usuarioDTO.isAdmin()) {
            saveAdmin(usuario);
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
    public void darExpCurso(String curso, int cantidadExp) {
        List<UsuarioDTO> alumnos = getAlumnosFromCurso(curso);
        for (UsuarioDTO alumno : alumnos) {

            int exp = alumno.getTarjetaAlumno().getExp();

            int nuevaExp = exp + cantidadExp;

            alumno.getTarjetaAlumno().setExp(nuevaExp);

            if (cantidadExp > 0) {
                hts.addMoreExpToHistorial(alumno.getId(), cantidadExp);
            } else {
                hts.addLessExpToHistorial(alumno.getId(), -cantidadExp);
            }

            saveUser(alumno);

            if ((double) (exp / 50) < (double) (nuevaExp / 50)) {
                int diferencia = (nuevaExp / 50) - (exp / 50);
                sumarElectronio(alumno.getId(), diferencia);
            }
        }
    }

    @Transactional
    public void darExpAlumnos(List<Long> idsAlumnos, int cantidadExp) {
        for (Long idAlumno : idsAlumnos) {
            UsuarioDTO alumno = getUserById(idAlumno);
            if (alumno != null) {

                int exp = alumno.getTarjetaAlumno().getExp();

                int nuevaExp = exp + cantidadExp;

                alumno.getTarjetaAlumno().setExp(nuevaExp);

                if (cantidadExp > 0) {
                    hts.addMoreExpToHistorial(alumno.getId(), cantidadExp);
                } else {
                    hts.addLessExpToHistorial(alumno.getId(), -cantidadExp);
                }

                saveUser(alumno);

                if ((double) (exp / 50) < (double) (nuevaExp / 50)) {
                    int diferencia = (nuevaExp / 50) - (exp / 50);
                    sumarElectronio(alumno.getId(), diferencia);
                }
            }
        }
    }

    @Transactional
    public void sumarElectronio(Long idAlumno, int electronios) {
        UsuarioDTO alumno = getUserById(idAlumno);
        byte electroniosAlumno = alumno.getTarjetaAlumno().getElectronios();
        alumno.getTarjetaAlumno().setElectronios((byte) (electronios + electroniosAlumno));
        saveUser(alumno);
    }
}