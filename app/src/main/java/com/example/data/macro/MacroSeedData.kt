package com.example.data.macro

/**
 * MacroSeedData: Catálogo base de macros pre-hechas que se inicializan en DocuSheet.
 * 
 * Diseñadas para casos de uso frecuentes de redacción en teléfonos móviles:
 * correspondencia formal, actas de acuerdos, memorándums ejecutivos, citas académicas,
 * tablas de planificación de tareas, acuerdos contractuales y reportes de avance.
 */
object MacroSeedData {

    val INITIAL_MACROS: List<MacroEntity> = listOf(
        MacroEntity(
            name = "Acta de Reunión Formal",
            description = "Plantilla completa con asistentes, acuerdos en tabla editorial y firmas.",
            triggerKeyword = ":acta:",
            category = "TRABAJO",
            templateContent = """# ACTA DE REUNIÓN: {TITULO}

**Fecha de sesión:** {FECHA}
**Hora de inicio:** {HORA}
**Formato de registro:** Hoja {FORMATO_HOJA}
**Redactor / Secretario:** {AUTOR}

---

### 1. ASISTENTES
• Convocante: {AUTOR}
• Integrantes: [Nombre 1], [Nombre 2], [Nombre 3]
• Ausentes justificados: [Ninguno / Especificar]

### 2. ORDEN DEL DÍA
1. Revisión de compromisos anteriores y estado de situación.
2. Análisis de propuestas y deliberación técnica.
3. Asignación de tareas y acuerdos vinculantes.

### 3. ACUERDOS Y COMPROMISOS ADOPTADOS
[table:editorial]
| N° | Compromiso / Acción | Responsable | Fecha Límite |
| 1 | Entrega de borrador inicial | {AUTOR} | Próxima sesión |
| 2 | Validación técnica | Equipo revisor | En 5 días |
| 3 | Aprobación final y archivo | Dirección | Fin de mes |
[/table]

> "Siendo las {HORA}, se da por concluida la sesión con la total conformidad de los comparecientes."

---

[align:center]
____________________________          ____________________________
       Firma Secretario                            Firma Presidente
[/align]""",
            isPredefined = true,
            iconName = "groups"
        ),

        MacroEntity(
            name = "Carta Formal / Oficio Editorial",
            description = "Estructura protocolar con fecha, destinatario, cuerpo justificado y firma.",
            triggerKeyword = ":carta:",
            category = "CORRESPONDENCIA",
            templateContent = """[align:right]
{FECHA}
[/align]

**A la atención de:**
[Nombre del Destinatario]
[Cargo o Institución]
[Ciudad / Despacho]

**ASUNTO:** {TITULO}

Estimado/a señor/a:

Por medio de la presente comunicación escrita, me dirijo a usted con el debido respeto para exponer lo siguiente:

[Escriba aquí los antecedentes y motivos principales de su solicitud o comunicado con claridad y precisión.]

Confiando en que la presente merezca su favorable acogida y agradeciendo de antemano el tiempo dedicado a su lectura, quedo a su entera disposición para cualquier aclaración complementaria.

Sin otro particular a que hacer referencia, le saluda atentamente,

[align:center]
___________________________________
{AUTOR}
DNI / Identificación: [___________]
Contacto: [Correo o Teléfono móvil]
[/align]""",
            isPredefined = true,
            iconName = "mail"
        ),

        MacroEntity(
            name = "Memorándum Ejecutivo",
            description = "Nota interna rápida con destinatario, remitente, fecha y casillas de tareas.",
            triggerKeyword = ":memo:",
            category = "TRABAJO",
            templateContent = """# MEMORÁNDUM EJECUTIVO

**PARA:** [Departamento / Destinatario]
**DE:** {AUTOR}
**FECHA:** {FECHA_CORTA} ({HORA})
**ASUNTO:** {TITULO}

---

### INSTRUCCIONES Y PRIORIDADES:
[ ] Revisar los puntos establecidos en la hoja de trabajo.
[ ] Distribuir la documentación a los integrantes del equipo.
[ ] Confirmar recepción y cumplimiento antes del cierre de jornada.

> "Agradecemos la máxima celeridad y compromiso con las directrices impartidas."

[align:right]
*Registrado por: {AUTOR}*
[/align]""",
            isPredefined = true,
            iconName = "assignment"
        ),

        MacroEntity(
            name = "Ficha de Lectura y Cita Académica",
            description = "Bloque de cita destacada con autor, fecha, superíndices y notaciones.",
            triggerKeyword = ":cita:",
            category = "ACADÉMICO",
            templateContent = """### FICHA EDITORIAL Y CITA TEXTUAL

> "{SELECCION}"

**Referencia bibliográfica:**
• Autor: [Apellido, Nombre]
• Obra / Publicación: *{TITULO}*
• Fecha de consulta: {FECHA}
• Localización: Pág. {PAGINA} (de {TOTAL_PAGINAS})
• Notación de referencia: Ref.<sup>[1]</sup>

**Análisis reflexivo:**
[Anotar aquí las interpretaciones críticas y aportes conceptuales del texto citado.]""",
            isPredefined = true,
            iconName = "format_quote"
        ),

        MacroEntity(
            name = "Tabla de Planificación Semanal",
            description = "Cuadrícula estructurada para seguimiento de tareas, responsables y plazos.",
            triggerKeyword = ":plan:",
            category = "TABLAS",
            templateContent = """### TABLA DE CONTROL Y PLANIFICACIÓN SEMANAL

[table:rayada]
| Tarea / Actividad | Responsable | Estado | Prioridad |
| Investigación preliminar | {AUTOR} | En curso | Alta |
| Redacción de contenidos | Redactor asignado | Pendiente | Media |
| Revisión editorial y estilo | Revisor | Pendiente | Alta |
| Publicación y archivo | Administración | Programada | Normal |
[/table]

*Planificado en formato {FORMATO_HOJA} — Total de palabras acumuladas: {TOTAL_PALABRAS}*""",
            isPredefined = true,
            iconName = "table_chart"
        ),

        MacroEntity(
            name = "Contrato / Acuerdo Breve",
            description = "Contrato básico con cláusulas numeradas, fecha y firmas yuxtapuestas.",
            triggerKeyword = ":contrato:",
            category = "LEGAL",
            templateContent = """# ACUERDO PRIVADO Y COMPROMISO MUTUO

En la fecha **{FECHA}**, se celebra el presente acuerdo entre las partes comparecientes, bajo las siguientes:

### CLÁUSULAS:
**PRIMERA (Objeto):** El presente documento establece las condiciones para el desarrollo y entrega de *{TITULO}*.

**SEGUNDA (Obligaciones):** Las partes se comprometen a respetar los plazos, la confidencialidad y los estándares acordados.

**TERCERA (Jurisdicción):** Para cualquier controversia, las partes se someten de común acuerdo al diálogo de buena fe.

En prueba de conformidad, se suscribe el presente acuerdo en dos ejemplares de un mismo tenor y efecto:

[align:center]
__________________________              __________________________
        PARTE A                                   PARTE B
    {AUTOR}                                  [Contraparte]
[/align]""",
            isPredefined = true,
            iconName = "gavel"
        ),

        MacroEntity(
            name = "Envolver en Cita Editorial de Autor",
            description = "Enmarca el texto seleccionado entre líneas decorativas y pie de autoría.",
            triggerKeyword = ":envolver:",
            category = "ESTILO",
            templateContent = """---

> "{SELECCION}"

[align:right]
*— {AUTOR} ({FECHA})*
[/align]

---""",
            isPredefined = true,
            iconName = "auto_awesome"
        ),

        MacroEntity(
            name = "Informe de Progreso y Avance",
            description = "Estructura de reporte ejecutivo con objetivos cumplidos, riesgos y métricas.",
            triggerKeyword = ":reporte:",
            category = "REPORTES",
            templateContent = """# INFORME DE PROGRESO: {TITULO}

**Fecha de emisión:** {FECHA} a las {HORA}
**Extensión del documento:** {TOTAL_PALABRAS} palabras (~{TOTAL_PAGINAS} pág.)
**Formato de página:** {FORMATO_HOJA}

---

### 1. OBJETIVOS CUMPLIDOS
• [Logro principal completado en el período de trabajo]
• [Meta cuantitativa o cualitativa alcanzada exitosamente]

### 2. INCIDENCIAS Y MITIGACIONES
• [Describir cualquier obstáculo detectado y cómo se resolvió]

### 3. PRÓXIMOS PASOS
• [Acción inmediata a ejecutar en la siguiente jornada de redacción]

[align:right]
*Elaborado por: {AUTOR}*
[/align]""",
            isPredefined = true,
            iconName = "insights"
        )
    )
}
