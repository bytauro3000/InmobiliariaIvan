package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.service.EmailVerificationService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final JavaMailSender mailSender;

    private static class VerificationData {
        final String pin;
        final long expiryMs;
        boolean verified;

        VerificationData(String pin, long expiryMs) {
            this.pin = pin;
            this.expiryMs = expiryMs;
            this.verified = false;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryMs;
        }
    }

    private static final long PIN_TTL_MS = 5 * 60 * 1000; // 5 minutos
    private static final int PIN_LENGTH = 6;
    private final Map<String, VerificationData> verificationStore = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    @Override
    public String enviarPin(String email) {
        // Generar PIN
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < PIN_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        String pin = sb.toString();

        // Guardar en memoria
        verificationStore.put(email.toLowerCase().trim(), new VerificationData(pin, System.currentTimeMillis() + PIN_TTL_MS));

        // Enviar email
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("Código de verificación - Inmobiliaria IVAN");
            helper.setText(construirCuerpo(pin), true);

            mailSender.send(mensaje);
            log.info("PIN de verificación enviado a {}", email);
        } catch (Exception e) {
            log.error("Error al enviar PIN a {}: {}", email, e.getMessage());
            verificationStore.remove(email.toLowerCase().trim());
            throw new RuntimeException("No se pudo enviar el código de verificación al correo.");
        }

        return pin;
    }

    @Override
    public boolean verificarPin(String email, String pin) {
        String key = email.toLowerCase().trim();
        VerificationData data = verificationStore.get(key);

        if (data == null) {
            return false;
        }

        if (data.isExpired()) {
            verificationStore.remove(key);
            return false;
        }

        if (!data.pin.equals(pin.trim())) {
            return false;
        }

        data.verified = true;
        return true;
    }

    @Override
    public boolean isEmailVerificado(String email) {
        String key = email.toLowerCase().trim();
        VerificationData data = verificationStore.get(key);
        return data != null && data.verified;
    }

    @Override
    public void limpiarVerificacion(String email) {
        verificationStore.remove(email.toLowerCase().trim());
    }

    private String construirCuerpo(String pin) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: Arial, sans-serif; background: #f4f6fb; padding: 30px;">
                <div style="max-width: 480px; margin: 0 auto; background: #fff; border-radius: 12px; padding: 32px; box-shadow: 0 4px 20px rgba(0,0,0,0.08);">
                    <div style="text-align: center; margin-bottom: 24px;">
                        <div style="width: 56px; height: 56px; background: #2ecc71; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center;">
                            <span style="color: #fff; font-size: 28px;">&#9993;</span>
                        </div>
                    </div>
                    <h2 style="text-align: center; color: #1a2332; margin: 0 0 8px;">Verificación de Correo</h2>
                    <p style="text-align: center; color: #64748b; margin: 0 0 24px;">Ingresa este código en el formulario de registro para verificar tu dirección de correo electrónico.</p>
                    <div style="text-align: center; background: #f0f2f5; border-radius: 10px; padding: 20px; margin-bottom: 24px;">
                        <span style="font-size: 36px; font-weight: 800; letter-spacing: 8px; color: #1a2332; font-family: 'Courier New', monospace;">""" + pin + """
                        </span>
                    </div>
                    <p style="text-align: center; color: #94a3b8; font-size: 13px; margin: 0;">Este código expira en 5 minutos. Si no solicitaste esta verificación, ignora este mensaje.</p>
                </div>
            </body>
            </html>
            """;
    }
}
