package edu.badpals.FigurasOcultas.service;

import edu.badpals.FigurasOcultas.model.dto.TarjetaAlumnoDTO;
import edu.badpals.FigurasOcultas.model.entity.TarjetaAlumno;
import edu.badpals.FigurasOcultas.model.repository.TarjetaAlumnoRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TarjetaAlumnoService {

    @Autowired
    private TarjetaAlumnoRepository tarjetaAlumnoRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Transactional
    public void addTarjetaAlumno(TarjetaAlumnoDTO tarjetaAlumnoDTO) {
        TarjetaAlumno tarjetaAlumno = modelMapper.map(tarjetaAlumnoDTO, TarjetaAlumno.class);
        tarjetaAlumnoRepository.save(tarjetaAlumno);
    }
}