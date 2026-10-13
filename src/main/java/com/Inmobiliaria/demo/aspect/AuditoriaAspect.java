package com.Inmobiliaria.demo.aspect;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.Inmobiliaria.demo.annotation.Auditable;
import com.Inmobiliaria.demo.service.AuditoriaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Intercepta los métodos anotados con @Auditable y registra la acción en
 * la tabla de auditoría una vez que el método termina con éxito.
 *
 * @Order(1): corre el más afuera para que, si hay otra transacción/timing,
 * el registro ocurra después del proceed().
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(1)
public class AuditoriaAspect {

    private final AuditoriaService auditoriaService;

    @Around("@annotation(auditable)")
    public Object auditar(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        Object resultado = joinPoint.proceed();

        try {
            Integer idEntidad = extraerId(joinPoint, auditable.indiceArgId());
            String descripcion = describir(joinPoint);
            auditoriaService.registrar(auditable.accion(), auditable.entidad(), idEntidad, descripcion);
        } catch (Throwable e) {
            // Nunca dejar que un fallo del aspecto comprometa el resultado del negocio.
            log.warn("Fallo no crítico en AuditoriaAspect sobre {}: {}", joinPoint.getSignature(), e.getMessage());
        }

        return resultado;
    }

    private Integer extraerId(ProceedingJoinPoint joinPoint, int indice) {
        if (indice < 0) {
            return null;
        }
        Object[] args = joinPoint.getArgs();
        if (indice >= args.length || args[indice] == null) {
            return null;
        }
        Object arg = args[indice];
        if (arg instanceof Number numero) {
            return numero.intValue();
        }
        // DTOs con getXxxId(): se intenta por reflexión el primer getter "idXxx"
        try {
            for (Method m : arg.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getName().startsWith("get")
                        && m.getName().toLowerCase().contains("id")
                        && m.getReturnType().equals(Integer.class)) {
                    Object valor = m.invoke(arg);
                    if (valor instanceof Number n) {
                        return n.intValue();
                    }
                }
            }
        } catch (Exception ignored) {
            // sin id: la auditoría sigue siendo válida sin id_entidad
        }
        return null;
    }

    private String describir(ProceedingJoinPoint joinPoint) {
        MethodSignature firma = (MethodSignature) joinPoint.getSignature();
        return "Operación " + firma.getDeclaringType().getSimpleName() + "#" + firma.getName();
    }
}
