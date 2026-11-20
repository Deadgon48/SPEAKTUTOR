---
lang: es
title: Política de Privacidad -- SpeakTutor
viewport: width=device-width, initial-scale=1.0
---

<div>

# 🗣️ SpeakTutor

Política de Privacidad · Última actualización: 19 de noviembre de 2026

</div>

::: {role="main"}
## 1. Identificación de la aplicación

**SpeakTutor: Terapia de Fluidez** ([com.itsx.speaktutor]{.badge}) es
una aplicación móvil de entrenamiento fonatorio para Android
desarrollada por **ITSX / Deadgon48**. La aplicación está diseñada como
herramienta de apoyo y práctica complementaria a la terapia del habla
profesional; no constituye un dispositivo médico ni reemplaza el
diagnóstico o tratamiento clínico.

## 2. Datos que la app procesa y dónde se almacenan

-   **Audio del micrófono:** Se captura en tiempo real exclusivamente
    para análisis acústico local (detección de bloqueos
    tónicos/clónicos, métricas de energía RMS y tasa de cruce por cero).
    El audio *no se transmite a ningún servidor externo*; los archivos
    de grabación de práctica se guardan únicamente en el almacenamiento
    interno del dispositivo (`filesDir`) y el usuario puede eliminarlos
    en cualquier momento.
-   **Datos de sesiones clínicas (historial de progreso):** Los
    registros de aciertos, errores, bloqueos y puntuaciones se guardan
    en `SharedPreferences` local del dispositivo. No se envían a la nube
    ni a servidores de terceros.
-   **Reconocimiento de voz (STT):** La app utiliza el
    `SpeechRecognizer` de Android, que puede utilizar los servicios de
    Google Speech-to-Text en la nube. Consulta la [Política de
    privacidad de
    Google](https://policies.google.com/privacy){target="_blank"} para
    saber cómo procesa ese audio Google LLC.
-   **Identificadores del dispositivo (ID de publicidad):** La app
    integra el SDK de Google AdMob
    (`com.google.android.gms:play-services-ads`), que puede recopilar el
    *Android Advertising ID (AAID)* con fines de publicidad
    personalizada. El usuario puede restablecer o inhabilitar este
    identificador en *Ajustes \> Google \> Anuncios* de su dispositivo.

## 3. Finalidad del tratamiento

-   Proporcionar retroalimentación de biofeedback acústico en tiempo
    real durante los ejercicios de fluidez.
-   Almacenar el historial de progreso local para que el motor
    adaptativo pueda personalizar los ejercicios.
-   Mostrar anuncios publicitarios mediante Google AdMob para financiar
    el desarrollo de la app.

## 4. Publicidad (Google AdMob)

SpeakTutor muestra banners publicitarios suministrados por **Google
AdMob**. AdMob puede utilizar el ID de publicidad del dispositivo para
mostrar anuncios personalizados basados en los intereses del usuario.
Para más información, consulta la [política de privacidad de
AdMob](https://support.google.com/admob/answer/6128543){target="_blank"}.

Si publicas en la Unión Europea o el Reino Unido, la app implementa el
**SDK Google UMP (User Messaging Platform)** para solicitar el
consentimiento del usuario antes de cargar anuncios personalizados, en
cumplimiento del RGPD/GDPR.

## 5. Datos compartidos con terceros

Tercero                 Dato compartido                       Finalidad
  ----------------------- ------------------------------------- --------------------------
Google AdMob            ID de publicidad (AAID)               Publicidad personalizada
Google Speech-to-Text   Audio de voz durante ejercicios STT   Reconocimiento de habla

Los datos clínicos locales (sesiones, progreso, grabaciones de práctica)
**no se comparten con terceros**.

## 6. Derechos del usuario

-   **Acceso y eliminación:** El usuario puede borrar todos los datos
    locales desinstalando la app o limpiando los datos de la app en
    Ajustes del dispositivo.
-   **Opt-out de publicidad personalizada:** El usuario puede
    inhabilitar la personalización de anuncios en *Ajustes \> Google \>
    Anuncios \> Inhabilitar la personalización de anuncios*.
-   **Usuarios UE/UK:** En el primer inicio se mostrará un diálogo de
    consentimiento de publicidad; el usuario puede cambiar su elección
    en cualquier momento desde los ajustes de la app.

## 7. Seguridad

Los datos almacenados localmente se encuentran en el espacio de
almacenamiento privado de la app, no accesible a otras aplicaciones sin
permisos de root. No se implementan servidores propios ni bases de datos
en la nube para almacenar información personal de los usuarios.

## 8. Menores de edad

SpeakTutor no está dirigida específicamente a menores de 13 años. Los
anuncios de AdMob están configurados para audiencia general. Si un padre
o tutor detecta que un menor ha registrado datos, puede eliminarlos
desinstalando la app.

## 9. Aviso médico / terapéutico

SpeakTutor es una herramienta de apoyo al entrenamiento fonatorio y
práctica personal. **No constituye un diagnóstico médico, tratamiento
clínico ni reemplaza la evaluación de un logopeda o fonoaudiólogo
certificado.** Su uso debe ser complementario a la terapia profesional.

## 10. Cambios en esta política

Cualquier cambio relevante en esta política de privacidad se comunicará
actualizando la fecha indicada al inicio de este documento. Se
recomienda revisarla periódicamente.

## 11. Contacto

Para preguntas sobre privacidad, escribe a: <privacidad@speaktutor.app>
o abre un issue en
[github.com/Deadgon48/SPEAKTUTOR](https://github.com/Deadgon48/SPEAKTUTOR){target="_blank"}.
:::

© 2026 SpeakTutor · ITSX / Deadgon48 · Todos los derechos reservados
