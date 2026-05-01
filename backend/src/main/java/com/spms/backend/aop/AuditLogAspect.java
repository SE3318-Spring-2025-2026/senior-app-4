package com.spms.backend.aop;

import com.spms.backend.annotation.AuditableOperation;
import com.spms.backend.model.ActionType;
import com.spms.backend.model.AuditLog;
import com.spms.backend.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;

    public AuditLogAspect(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @AfterReturning(pointcut = "@annotation(auditableOperation)", returning = "result")
    public void logAuditActivity(JoinPoint joinPoint, AuditableOperation auditableOperation, Object result) {
        ActionType actionType = auditableOperation.actionType();
        String eventDetails = auditableOperation.eventDetails();

        if ("handleAdvisorRequestDecision".equals(joinPoint.getSignature().getName())) {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length >= 3 && args[2] instanceof String) {
                String status = (String) args[2];
                if ("approved".equalsIgnoreCase(status)) {
                    actionType = ActionType.ADVISOR_APPROVED;
                } else if ("rejected".equalsIgnoreCase(status)) {
                    actionType = ActionType.ADVISOR_REJECTED;
                }
            }
        }

        HttpServletRequest request = getRequest();
        String ipAddress = request != null ? request.getRemoteAddr() : "unknown";
        Long userId = 0L;

        if (request != null && request.getAttribute("jwt_userId") != null) {
            userId = Long.valueOf(request.getAttribute("jwt_userId").toString());
        }

        Long groupId = extractGroupId(joinPoint, result);
        Long committeeId = extractCommitteeId(joinPoint, result);

        AuditLog log = new AuditLog();
        log.setActionType(actionType);
        log.setUserId(userId);
        log.setIpAddress(ipAddress);
        log.setGroupId(groupId);
        log.setCommitteeId(committeeId);

        if (eventDetails == null || eventDetails.isEmpty()) {
            log.setEventDetails(actionType.name() + " operation performed by User ID: " + userId);
        } else {
            log.setEventDetails(eventDetails);
        }

        auditLogRepository.save(log);
    }

    private Long extractGroupId(JoinPoint joinPoint, Object result) {
        // Try returning DTO first (e.g. createGroup)
        if (result != null) {
            try {
                // GroupResponseDto has record style accessor
                Method getIdMethod = result.getClass().getMethod("id");
                Object idObj = getIdMethod.invoke(result);
                if (idObj instanceof Long) {
                    return (Long) idObj;
                }
            } catch (Exception ignored) {}

            try {
                Method getIdMethod = result.getClass().getMethod("getId");
                Object idObj = getIdMethod.invoke(result);
                if (idObj instanceof Long) {
                    return (Long) idObj;
                }
            } catch (Exception ignored) {}
        }

        // Check method args
        if (joinPoint.getSignature() instanceof MethodSignature) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            if (parameterNames != null && args != null) {
                for (int i = 0; i < parameterNames.length; i++) {
                    if ("groupId".equals(parameterNames[i]) && args[i] instanceof Long) {
                        return (Long) args[i];
                    }
                }
            }
        }
        return null;
    }

    private Long extractCommitteeId(JoinPoint joinPoint, Object result) {
        // Try returning DTO first (e.g. createCommittee)
        if (result != null) {
            try {
                Method getIdMethod = result.getClass().getMethod("committeeId");
                Object idObj = getIdMethod.invoke(result);
                if (idObj instanceof Long) {
                    return (Long) idObj;
                }
            } catch (Exception ignored) {}

            try {
                Method getIdMethod = result.getClass().getMethod("getCommitteeId");
                Object idObj = getIdMethod.invoke(result);
                if (idObj instanceof Long) {
                    return (Long) idObj;
                }
            } catch (Exception ignored) {}
        }

        // Check method args
        if (joinPoint.getSignature() instanceof MethodSignature) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            if (parameterNames != null && args != null) {
                for (int i = 0; i < parameterNames.length; i++) {
                    if ("committeeId".equals(parameterNames[i]) && args[i] instanceof Long) {
                        return (Long) args[i];
                    }
                }
            }
        }
        return null;
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
