package com.agroempresa.erp.cliente;

import com.agroempresa.erp.cliente.dto.ClienteRequest;
import com.agroempresa.erp.cliente.dto.ClienteResponse;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.common.error.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return clienteRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(ClienteResponse::desdeEntidad)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listarActivos() {
        return clienteRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(ClienteResponse::desdeEntidad)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorId(Long id) {
        Cliente cliente = buscarClientePorId(id);
        return ClienteResponse.desdeEntidad(cliente);
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        validarRequest(request);

        String nombre = normalizar(request.nombre());
        String documentoIdentidad = normalizar(request.documentoIdentidad());

        validarDocumentoDisponible(documentoIdentidad, null);

        Cliente cliente = new Cliente(
                nombre,
                documentoIdentidad,
                normalizar(request.telefono()),
                normalizar(request.email()),
                normalizar(request.direccion())
        );

        Cliente clienteGuardado = clienteRepository.save(cliente);

        return ClienteResponse.desdeEntidad(clienteGuardado);
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest request) {
        validarRequest(request);

        Cliente cliente = buscarClientePorId(id);

        String nombre = normalizar(request.nombre());
        String documentoIdentidad = normalizar(request.documentoIdentidad());

        validarDocumentoDisponible(documentoIdentidad, id);

        cliente.actualizar(
                nombre,
                documentoIdentidad,
                normalizar(request.telefono()),
                normalizar(request.email()),
                normalizar(request.direccion())
        );

        return ClienteResponse.desdeEntidad(cliente);
    }

    @Transactional
    public ClienteResponse desactivar(Long id) {
        Cliente cliente = buscarClientePorId(id);

        if (!cliente.getActivo()) {
            throw new BusinessException("El cliente ya se encuentra desactivado");
        }

        cliente.desactivar();

        return ClienteResponse.desdeEntidad(cliente);
    }

    private Cliente buscarClientePorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el cliente con id: " + id));
    }

    private void validarRequest(ClienteRequest request) {
        if (request == null) {
            throw new BusinessException("Los datos del cliente son obligatorios");
        }

        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new BusinessException("El nombre del cliente es obligatorio");
        }
    }

    private void validarDocumentoDisponible(String documentoIdentidad, Long idActual) {
        if (documentoIdentidad == null) {
            return;
        }

        clienteRepository.findByDocumentoIdentidadIgnoreCase(documentoIdentidad)
                .filter(cliente -> !cliente.getId().equals(idActual))
                .ifPresent(cliente -> {
                    throw new BusinessException("Ya existe un cliente con el documento: " + documentoIdentidad);
                });
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }
}
