package com.Inmobiliaria.demo.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.Inmobiliaria.demo.dto.LoginResponse;
import com.Inmobiliaria.demo.entity.Cliente;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.repository.ClienteRepository;
import com.Inmobiliaria.demo.security.JwtUtil;
import com.Inmobiliaria.demo.service.PortalAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * Login del portal del cliente con Google Identity Services.
 *
 * Flujo: el frontend recibe el ID token de Google → lo envía aquí →
 * se verifica contra el endpoint oficial de Google (tokeninfo) validando
 * audiencia, issuer y email verificado → se busca al cliente por email →
 * se emite el JWT propio del portal (ROLE_CLIENTE, 12 h).
 *
 * tokeninfo es el endpoint público que Google expone exactamente para esto:
 * verifica la firma del JWT en los servidores de Google y devuelve el payload.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalAuthServiceImpl implements PortalAuthService {

    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo";

    private final ClienteRepository clienteRepository;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${google.oauth.client-id:}")
    private String googleClientId;

    private final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse autenticarConGoogle(String credential) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new NegocioException("Portal no configurado: falta GOOGLE_CLIENT_ID en el servidor.");
        }
        if (credential == null || credential.isBlank()) {
            throw new NegocioException("Credencial de Google ausente. Intente de nuevo.");
        }

        Map<String, Object> payload = verificarTokenGoogle(credential);

        String email = (String) payload.get("email");
        if (email == null || email.isBlank()) {
            throw new NegocioException("La cuenta de Google no entregó un correo electrónico.");
        }

        List<Cliente> clientes = clienteRepository.findByEmailIgnoreCase(email);
        if (clientes.isEmpty()) {
            log.info("Portal: email no registrado como cliente: {}", email);
            throw new NegocioException("Correo no registrado. Consulte con la inmobiliaria.");
        }

        Cliente principal = clientes.get(0);
        if (clientes.size() > 1) {
            log.warn("Portal: email con {} registros de cliente ({}); se usa el primero (id={})",
                    clientes.size(), email, principal.getIdCliente());
        }

        String token = jwtUtil.generateTokenCliente(principal, email.trim().toLowerCase());
        log.info("Portal: login exitoso id={} email={}", principal.getIdCliente(), email);
        return new LoginResponse(token);
    }

    /** Verifica el ID token de Google y devuelve su payload validado. */
    private Map<String, Object> verificarTokenGoogle(String credential) {
        String url = UriComponentsBuilder.fromHttpUrl(TOKENINFO_URL)
                .queryParam("id_token", credential)
                .toUriString();

        Map<String, Object> payload;
        try {
            String body = restTemplate.getForObject(url, String.class);
            payload = objectMapper.readValue(body, Map.class);
        } catch (Exception e) {
            log.warn("Portal: token de Google inválido: {}", e.getMessage());
            throw new NegocioException("Sesión de Google inválida o expirada. Intente de nuevo.");
        }

        // Audiencia: el token debe estar emitido para NUESTRO Client ID
        String aud = payload.get("aud") != null ? payload.get("aud").toString() : null;
        if (!googleClientId.equals(aud)) {
            log.warn("Portal: audiencia incorrecta. aud={} esperado={}", aud, googleClientId);
            throw new NegocioException("Sesión de Google inválida. Intente de nuevo.");
        }

        // Issuer: cuenta de Google (no una cuenta falsificada)
        String iss = payload.get("iss") != null ? payload.get("iss").toString() : null;
        if (iss == null || !iss.startsWith("https://accounts.google.com")) {
            log.warn("Portal: issuer incorrecto: {}", iss);
            throw new NegocioException("Sesión de Google inválida. Intente de nuevo.");
        }

        // Email verificado por Google (impide suplantación con cuentas sin verificar)
        Object verificado = payload.get("email_verified");
        if (verificado != null && "false".equals(verificado.toString())) {
            throw new NegocioException("El correo de Google no está verificado.");
        }

        return payload;
    }
}
