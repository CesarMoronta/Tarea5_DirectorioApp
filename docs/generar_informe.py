"""
Genera docs/Informe_Tarea5_Directorio.pdf con reportlab.

Uso (desde la carpeta del proyecto):
    python docs/generar_informe.py "https://github.com/usuario/repositorio"
"""
import re
import sys
import textwrap
from datetime import date
from pathlib import Path

from reportlab.graphics.shapes import Drawing, Line, Polygon, Rect, String
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    Image, KeepTogether, PageBreak, Paragraph, Preformatted, SimpleDocTemplate, Spacer, Table, TableStyle,
)

RAIZ = Path(__file__).resolve().parents[1]
SRC = RAIZ / "app" / "src" / "main" / "java" / "com" / "cmoronta" / "directorio"
CAPTURAS = RAIZ / "docs" / "capturas"
SALIDA = RAIZ / "docs" / "Informe_Tarea5_Directorio.pdf"
REPO = sys.argv[1] if len(sys.argv) > 1 else "(pegar aquí el enlace al repositorio)"

# ---------- Fuentes (Arial/Consolas de Windows: soportan acentos y flechas) ----------
FUENTES = Path("C:/Windows/Fonts")
pdfmetrics.registerFont(TTFont("Arial", str(FUENTES / "arial.ttf")))
pdfmetrics.registerFont(TTFont("Arial-Bold", str(FUENTES / "arialbd.ttf")))
pdfmetrics.registerFont(TTFont("Mono", str(FUENTES / "consola.ttf")))
pdfmetrics.registerFontFamily("Arial", normal="Arial", bold="Arial-Bold")
pdfmetrics.registerFont(TTFont("TNR", str(FUENTES / "times.ttf")))
pdfmetrics.registerFont(TTFont("TNR-Bold", str(FUENTES / "timesbd.ttf")))
pdfmetrics.registerFontFamily("TNR", normal="TNR", bold="TNR-Bold")

AZUL = colors.HexColor("#1E3A8A")
AZUL_CLARO = colors.HexColor("#DBEAFE")
GRIS = colors.HexColor("#475569")

base = getSampleStyleSheet()
E = {
    "titulo": ParagraphStyle("titulo", parent=base["Title"], fontName="TNR-Bold", fontSize=22, textColor=AZUL, leading=28),
    "sub": ParagraphStyle("sub", parent=base["Normal"], fontName="TNR", fontSize=12, alignment=TA_CENTER, textColor=GRIS, leading=18),
    "h1": ParagraphStyle("h1", parent=base["Heading1"], fontName="TNR-Bold", fontSize=16, textColor=AZUL, spaceBefore=6, spaceAfter=8),
    "h2": ParagraphStyle("h2", parent=base["Heading2"], fontName="TNR-Bold", fontSize=12.5, textColor=AZUL, spaceBefore=8, spaceAfter=4),
    "p": ParagraphStyle("p", parent=base["Normal"], fontName="TNR", fontSize=11.5, leading=16, spaceAfter=6),
    "li": ParagraphStyle("li", parent=base["Normal"], fontName="TNR", fontSize=11.5, leading=16, leftIndent=14, bulletIndent=4),
    "celda": ParagraphStyle("celda", parent=base["Normal"], fontName="TNR", fontSize=10, leading=13),
    "pie": ParagraphStyle("pie", parent=base["Normal"], fontName="TNR", fontSize=8.5, alignment=TA_CENTER, textColor=GRIS),
    "code": ParagraphStyle("code", fontName="Mono", fontSize=7.6, leading=9.6, backColor=colors.HexColor("#F1F5F9"),
                           borderColor=colors.HexColor("#CBD5E1"), borderWidth=0.5, borderPadding=6, spaceBefore=4, spaceAfter=10),
}


def p(texto, estilo="p"):
    return Paragraph(texto, E[estilo])


def vinetas(items):
    return [Paragraph(t, E["li"], bulletText="•") for t in items]


def tabla(filas, anchos, encabezado=True):
    datos = [[Paragraph(str(c), E["celda"]) for c in fila] for fila in filas]
    t = Table(datos, colWidths=anchos, repeatRows=1 if encabezado else 0)
    estilo = [
        ("GRID", (0, 0), (-1, -1), 0.4, colors.HexColor("#CBD5E1")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 5), ("RIGHTPADDING", (0, 0), (-1, -1), 5),
    ]
    if encabezado:
        estilo += [("BACKGROUND", (0, 0), (-1, 0), AZUL_CLARO)]
    t.setStyle(TableStyle(estilo))
    return t


def codigo(texto: str):
    return Preformatted(textwrap.dedent(texto.strip("\n")).strip("\n"), E["code"])


def leer(ruta: Path) -> str:
    return ruta.read_text(encoding="utf-8")


def fragmento(ruta: Path, desde: str, hasta: str | None = None, incluir_hasta=False) -> str:
    """Extrae del archivo real el texto entre dos marcas (así el PDF siempre refleja el código actual)."""
    texto = leer(ruta)
    i = texto.index(desde)
    if hasta is None:
        return texto[i:]
    j = texto.index(hasta, i + len(desde))
    return texto[i: j + (len(hasta) if incluir_hasta else 0)]


def sin_imports(texto: str) -> str:
    return re.sub(r"^(package|import) .*\n", "", texto, flags=re.M).strip()


# ---------- Diagrama de arquitectura (dibujado con formas de reportlab) ----------

def diagrama() -> Drawing:
    W, H = 17 * cm, 12.2 * cm
    d = Drawing(W, H)

    def caja(x, y, w, h, texto, fondo, borde=AZUL, tam=8.5, negrita=False, lineas=None):
        d.add(Rect(x, y, w, h, rx=5, ry=5, fillColor=fondo, strokeColor=borde, strokeWidth=0.8))
        partes = lineas or [texto]
        total = len(partes) * (tam + 2)
        for k, linea in enumerate(partes):
            d.add(String(x + w / 2, y + h / 2 + total / 2 - (k + 1) * (tam + 2) + 2.5, linea,
                         fontName="Arial-Bold" if (negrita and k == 0) else "Arial", fontSize=tam if k == 0 else tam - 1,
                         textAnchor="middle", fillColor=colors.black))

    def flecha(x1, y1, x2, y2, color=GRIS):
        d.add(Line(x1, y1, x2, y2, strokeColor=color, strokeWidth=0.9))
        import math
        ang = math.atan2(y2 - y1, x2 - x1)
        a, b = 6, 3
        p1 = (x2 - a * math.cos(ang) + b * math.sin(ang), y2 - a * math.sin(ang) - b * math.cos(ang))
        p2 = (x2 - a * math.cos(ang) - b * math.sin(ang), y2 - a * math.sin(ang) + b * math.cos(ang))
        d.add(Polygon([x2, y2, *p1, *p2], fillColor=color, strokeColor=color))

    def banda(y, h, titulo):
        d.add(Rect(0, y, W, h, fillColor=colors.HexColor("#F8FAFC"), strokeColor=colors.HexColor("#CBD5E1"), strokeWidth=0.5))
        d.add(String(6, y + h - 11, titulo, fontName="Arial-Bold", fontSize=8, fillColor=GRIS))

    # Bandas (de arriba a abajo)
    banda(9.0 * cm, 3.0 * cm, "PRESENTACIÓN — Jetpack Compose")
    banda(6.2 * cm, 2.4 * cm, "VIEWMODELS — StateFlow<EstadoUi>")
    banda(4.1 * cm, 1.75 * cm, "REPOSITORIO")
    banda(1.3 * cm, 2.45 * cm, "DATOS")

    cw, ch = 4.6 * cm, 1.05 * cm
    xs = [0.9 * cm, 6.2 * cm, 11.5 * cm]
    caja(6.2 * cm, 10.65 * cm, cw, 0.85 * cm, "", AZUL_CLARO, lineas=["MainActivity + NavHost"], negrita=True)
    pantallas = ["UsuariosScreen", "DetalleScreen", "FavoritosScreen"]
    vms = ["UsuariosViewModel", "DetalleViewModel", "FavoritosViewModel"]
    for x, s, v in zip(xs, pantallas, vms):
        caja(x, 9.25 * cm, cw, ch, "", colors.white, lineas=[s], negrita=True)
        caja(x, 6.55 * cm, cw, ch + 0.25 * cm, "", colors.white, lineas=[v, "init { cargar() }"], negrita=True)
        flecha(x + cw * 0.35, 9.25 * cm, x + cw * 0.35, 6.55 * cm + ch + 0.25 * cm)               # eventos
        flecha(x + cw * 0.65, 6.55 * cm + ch + 0.25 * cm, x + cw * 0.65, 9.25 * cm, AZUL)        # estado
        flecha(6.2 * cm + cw / 2, 10.65 * cm, x + cw / 2, 9.25 * cm + ch)
    d.add(String(xs[0] + cw * 0.35 - 3, 8.72 * cm, "eventos", fontName="Arial", fontSize=6.5, textAnchor="end", fillColor=GRIS))
    d.add(String(xs[0] + cw * 0.65 + 3, 8.72 * cm, "estado", fontName="Arial", fontSize=6.5, fillColor=AZUL))

    rx, rw = 4.6 * cm, 7.8 * cm
    caja(rx, 4.35 * cm, rw, 1.15 * cm, "", AZUL_CLARO,
         lineas=["UsuarioRepositorio", "única fuente de verdad · errores → Resultado"], negrita=True)
    for x in xs:
        flecha(x + cw / 2, 6.55 * cm, rx + rw / 2 + (x - xs[1]) * 0.35, 4.35 * cm + 1.15 * cm)

    datos = [("Retrofit + OkHttp", "JsonPlaceholderApi"), ("Room (SQLite)", "favoritos + publicaciones"),
             ("MonitorConexion", "ConnectivityManager")]
    for x, (a, b) in zip(xs, datos):
        caja(x, 1.6 * cm, cw, 1.2 * cm, "", colors.white, lineas=[a, b], negrita=True)
        flecha(rx + rw / 2 + (x - xs[1]) * 0.35, 4.35 * cm, x + cw / 2, 1.6 * cm + 1.2 * cm)

    caja(xs[0], 0.05 * cm, cw, 0.9 * cm, "", colors.HexColor("#FEF3C7"), borde=colors.HexColor("#B45309"),
         lineas=["jsonplaceholder.typicode.com"])
    flecha(xs[0] + cw / 2, 1.6 * cm, xs[0] + cw / 2, 0.95 * cm, colors.HexColor("#B45309"))
    d.add(String(xs[0] + cw / 2 + 4, 1.2 * cm, "HTTPS", fontName="Arial", fontSize=6.5, fillColor=colors.HexColor("#B45309")))
    return d


# ---------- Capturas ----------

def captura(nombre, titulo, ancho=4.9 * cm):
    img = Image(str(CAPTURAS / nombre), width=ancho, height=ancho * 2400 / 1080)
    return [img, Spacer(1, 3), Paragraph(titulo, E["pie"])]


def fila_capturas(items):
    celdas = [captura(n, t) for n, t in items]
    while len(celdas) < 3:
        celdas.append("")
    t = Table([celdas], colWidths=[5.6 * cm] * 3)
    t.setStyle(TableStyle([("ALIGN", (0, 0), (-1, -1), "CENTER"), ("VALIGN", (0, 0), (-1, -1), "TOP")]))
    return t


def pie_pagina(canvas, doc):
    """Número de página centrado abajo, como en el modelo de UTESA."""
    canvas.saveState()
    canvas.setFont("TNR", 11)
    canvas.drawCentredString(letter[0] / 2, 1.2 * cm, str(doc.page))
    canvas.restoreState()


MESES = ["Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto",
         "Septiembre", "Octubre", "Noviembre", "Diciembre"]


def portada():
    """Portada según el modelo de UTESA."""
    hoy = date.today()
    centro = ParagraphStyle("pc", fontName="TNR", fontSize=12, leading=15, alignment=TA_CENTER)
    negrita = ParagraphStyle("pcb", parent=centro, fontName="TNR-Bold")
    uni = ParagraphStyle("uni", parent=negrita, fontSize=15, leading=19)

    def campo(etiqueta, valor):
        return [Paragraph(etiqueta, negrita), Paragraph(valor, centro), Spacer(1, 5)]

    return [
        Spacer(1, 1.2 * cm),
        Image(str(RAIZ / "docs" / "utesa_logo.png"), width=5.6 * cm, height=5.6 * cm),
        Spacer(1, 0.9 * cm),
        Paragraph("Universidad Tecnológica de Santiago", uni),
        Spacer(1, 4),
        Paragraph("“Tarea 5 – Actividad integradora: app que consume un servicio web”", centro),
        Spacer(1, 1.3 * cm),
        *campo("Nombre:", "César Miguel Moronta Guillen"),
        *campo("Matricula:", "2-22-0299"),
        *campo("Carrera:", "Ingeniería en Sistemas Computacionales"),
        *campo("Asignatura:", "Programación de Dispositivos Móviles"),
        *campo("Profesor:", "Joan Miguel Tejada Caba"),
        Spacer(1, 1.6 * cm),
        Paragraph(f"{hoy.day} de {MESES[hoy.month - 1]} del {hoy.year}", negrita),
        Spacer(1, 6),
        Paragraph("Santiago, Republica Dominicana", negrita),
        PageBreak(),
    ]


# ---------- Documento ----------

def construir():
    h = portada()
    # 1. Descripción
    h += [p("1. Descripción de la app", "h1"),
          p("<b>Directorio</b> es una app Android que consume la API pública <b>JSONPlaceholder</b>. Muestra la lista de "
            "usuarios, el detalle de cada uno con sus publicaciones, y permite marcar favoritos que se guardan en el "
            "teléfono con Room para consultarlos sin conexión. La interfaz informa siempre en qué estado está "
            "(cargando, éxito, error o sin Internet) y nunca se cierra por un fallo de red."),
          tabla([
              ["Requisito", "Implementación"],
              ["1. Lista de usuarios", "GET /users → nombre, correo y company.name (UsuariosScreen)"],
              ["2. Detalle y navegación", "Al tocar un usuario: datos completos + GET /posts?userId={id} (DetalleScreen, Navigation Compose)"],
              ["3. Favoritos persistentes", "Room: tablas favoritos y publicaciones. Se ven sin conexión (FavoritosScreen)"],
              ["4. Estados de la interfaz", "EstadoUi: Cargando, Exito, Error, SinConexion + botón Reintentar; reintento automático al volver Internet"],
              ["5. Librerías y permisos", "Retrofit 2.11 + Gson + OkHttp. Permisos INTERNET y ACCESS_NETWORK_STATE en el AndroidManifest"],
              ["6. Arquitectura", "MVVM por capas (sección 2) y relación con el ciclo de vida (sección 3)"],
          ], [4.2 * cm, 12.3 * cm]),
          ]

    # 2. Arquitectura
    h += [PageBreak(), p("2. Arquitectura: MVVM por capas", "h1"), diagrama(), Spacer(1, 8)]
    h += vinetas([
        "<b>Presentación (Compose):</b> las pantallas solo dibujan el estado y envían eventos (tocar, reintentar, "
        "marcar favorito). No conocen Retrofit ni Room.",
        "<b>ViewModel:</b> mantiene el estado de cada pantalla en un <font face='Mono'>StateFlow&lt;EstadoUi&gt;</font> "
        "y sobrevive a la rotación. Lanza las peticiones en <font face='Mono'>viewModelScope</font>.",
        "<b>Repositorio:</b> única fuente de verdad. Decide si los datos vienen de la API o de Room y convierte toda "
        "excepción en un <font face='Mono'>Resultado</font> (Exito, SinConexion o Error).",
        "<b>Datos:</b> Retrofit (remoto), Room (local) y MonitorConexion (estado de la red con ConnectivityManager).",
        "<b>Dependencias:</b> se crean una sola vez en la clase Application (<font face='Mono'>DirectorioApp</font>) "
        "con un contenedor manual.",
    ])
    h += [Spacer(1, 6), p("Paquetes", "h2"), codigo("""
com.cmoronta.directorio
├── DirectorioApp.kt          Application + contenedor de dependencias
├── MainActivity.kt           única Activity, NavHost (usuarios, detalle/{id}, favoritos)
├── data/modelo               Usuario, Publicacion, Resultado
├── data/remoto               JsonPlaceholderApi (Retrofit), DTOs, ClienteRetrofit
├── data/local                Room: FavoritoEntity, PublicacionEntity, FavoritoDao, BaseDatos
├── data/red                  MonitorConexion
├── data/repositorio          UsuarioRepositorio
└── ui/                       comun (estados), usuarios, detalle, favoritos, tema
""")]

    # 3. Ciclo de vida
    h += [PageBreak(), p("3. Ciclo de vida y carga de datos", "h1"),
          p("La carga de datos se relaciona con el ciclo de vida de Android de la siguiente forma:"),
          tabla([
              ["Momento", "Qué ocurre con los datos"],
              ["Application.onCreate()", "Se crean Retrofit, Room y el repositorio una sola vez (DirectorioApp)."],
              ["Activity.onCreate() → setContent", "Se dibuja la navegación; cada pantalla obtiene su ViewModel con viewModel(factory = …)."],
              ["Creación del ViewModel (init {})", "Se lanza cargar() en viewModelScope. La pantalla muestra el estado Cargando."],
              ["onStart … onStop", "collectAsStateWithLifecycle() observa el StateFlow solo mientras la pantalla está visible; en onStop deja de observar."],
              ["onResume", "LifecycleEventEffect(ON_RESUME): si la última carga falló (error o sin conexión), se reintenta al volver a la app."],
              ["Rotación / cambio de configuración", "La Activity se recrea pero el ViewModel sobrevive: no se repite la descarga."],
              ["Muerte del proceso", "El id del detalle se recupera de SavedStateHandle (argumento de navegación detalle/{id})."],
              ["ViewModel.onCleared()", "Al salir de la pantalla se cancela viewModelScope y cualquier petición en curso."],
          ], [5.2 * cm, 11.3 * cm]),
          Spacer(1, 8),
          p("Además, si la lista está en estado <b>SinConexion</b> y vuelve Internet, MonitorConexion lo detecta y el "
            "ViewModel recarga automáticamente."),
          p("Fragmento: carga en init {} y reintento en ON_RESUME", "h2"),
          codigo(fragmento(SRC / "ui/usuarios/UsuariosViewModel.kt", "    init {", "    fun alternarFavorito")),
          codigo(fragmento(SRC / "ui/usuarios/UsuariosScreen.kt", "    // collectAsStateWithLifecycle",
                           "viewModel.alReanudar() }", incluir_hasta=True)),
          ]

    # 4. Capturas
    h += [PageBreak(), p("4. Capturas", "h1"), p("Los tres estados de la interfaz", "h2"),
          fila_capturas([("1_cargando.png", "Carga"), ("2_lista_usuarios.png", "Éxito: lista de usuarios"),
                         ("6_sin_conexion.png", "Error / sin conexión")]),
          PageBreak(), p("Detalle y favoritos", "h2"),
          fila_capturas([("3_detalle_usuario.png", "Detalle con publicaciones"), ("4_lista_con_favoritos.png", "Favoritos marcados"),
                         ("5_favoritos.png", "Pantalla de favoritos")]),
          PageBreak(), p("Favoritos sin conexión (datos desde Room)", "h2"),
          fila_capturas([("7_favoritos_sin_conexion.png", "Favoritos sin Internet"),
                         ("8_detalle_sin_conexion.png", "Detalle y publicaciones sin Internet")]),
          ]

    # 5. Código API
    h += [PageBreak(), p("5. Código del consumo de la API (Retrofit)", "h1"),
          p("Permisos en AndroidManifest.xml", "h2"),
          codigo(fragmento(RAIZ / "app/src/main/AndroidManifest.xml", "    <!-- Necesario", "\n\n    <application")),
          p("Interfaz Retrofit y DTOs (data/remoto/JsonPlaceholderApi.kt)", "h2"),
          codigo(sin_imports(fragmento(SRC / "data/remoto/JsonPlaceholderApi.kt", "interface JsonPlaceholderApi", "// ---- DTOs"))),
          p("Cliente Retrofit (data/remoto/ClienteRetrofit.kt)", "h2"),
          codigo(fragmento(SRC / "data/remoto/ClienteRetrofit.kt", "object ClienteRetrofit")),
          p("Manejo de errores en el repositorio: la app nunca se cierra", "h2"),
          codigo(fragmento(SRC / "data/repositorio/UsuarioRepositorio.kt", "    suspend fun obtenerUsuarios()", "    suspend fun obtenerUsuario(id")),
          codigo(fragmento(SRC / "data/repositorio/UsuarioRepositorio.kt", "    /** Ejecuta una llamada", "\n}\n", incluir_hasta=True)),
          p("Estados de la interfaz", "h2"),
          codigo(fragmento(SRC / "ui/comun/EstadoUi.kt", "/** Los estados", "\nfun <T>")),
          codigo(fragmento(SRC / "ui/usuarios/UsuariosScreen.kt", "            when (val e = estado)", "\n        }\n    }\n}")),
          ]

    # 6. Persistencia
    h += [PageBreak(), p("6. Código de la persistencia (Room)", "h1"),
          p("Entidades, DAO y base de datos (data/local/BaseDatos.kt)", "h2"),
          codigo(fragmento(SRC / "data/local/BaseDatos.kt", "/** Usuario marcado", "@Dao")),
          codigo(fragmento(SRC / "data/local/BaseDatos.kt", "@Dao", "@Database")),
          codigo(fragmento(SRC / "data/local/BaseDatos.kt", "@Database")),
          p("Favoritos y publicaciones sin conexión (repositorio)", "h2"),
          codigo(fragmento(SRC / "data/repositorio/UsuarioRepositorio.kt", "    /** Publicaciones del usuario", "    /** Ejecuta una llamada")),
          ]

    # 7. Pruebas y conclusiones
    h += [PageBreak(), p("7. Pruebas", "h1"),
          p("Pruebas unitarias del repositorio con dobles de prueba (API, DAO y conexión falsos). Resultado: <b>5 de 5 pruebas superadas</b>."),
          tabla([["Prueba", "Qué verifica"],
                 ["lista de usuarios exitosa", "Nombre, correo y company.name se mapean correctamente"],
                 ["sin internet devuelve SinConexion", "Una IOException sin red no cierra la app: devuelve el estado SinConexion"],
                 ["error del servidor", "Un HTTP 500 se convierte en un mensaje claro para el usuario"],
                 ["favorito sin conexión", "Tras guardar un favorito y perder Internet, el usuario y sus publicaciones se leen de Room"],
                 ["quitar favorito", "Al quitarlo se borran también sus publicaciones guardadas"]],
                [5.2 * cm, 11.3 * cm]),
          p("8. Conclusiones", "h1")]
    h += vinetas([
        "Separar la app en capas (UI, ViewModel, repositorio y datos) permitió cambiar el origen de los datos (API o Room) "
        "sin tocar las pantallas, y probar el repositorio sin un teléfono ni conexión real.",
        "Modelar los estados de la interfaz como un tipo sellado (EstadoUi) obliga a contemplar la carga, el error y la "
        "falta de Internet en cada pantalla; el usuario siempre recibe un mensaje claro y un botón para reintentar.",
        "Capturar las excepciones en un solo punto del repositorio (IOException, HttpException y otras) evita que la app "
        "se cierre por fallos de red o del servidor.",
        "El ViewModel resuelve el problema del ciclo de vida: los datos se cargan en init {}, sobreviven a la rotación, y "
        "collectAsStateWithLifecycle solo observa mientras la pantalla está visible.",
        "Guardar en Room no solo el favorito sino también sus publicaciones hace que la experiencia sin conexión sea completa.",
    ])
    h += [Spacer(1, 10), p("9. Enlace al repositorio", "h1"), p(f"<link href='{REPO}' color='blue'>{REPO}</link>")]

    doc = SimpleDocTemplate(str(SALIDA), pagesize=letter, leftMargin=2 * cm, rightMargin=2 * cm,
                            topMargin=1.8 * cm, bottomMargin=1.8 * cm,
                            title="Tarea 5 - App de directorio con JSONPlaceholder", author="César Moronta")
    doc.build(h, onFirstPage=pie_pagina, onLaterPages=pie_pagina)
    print("PDF generado:", SALIDA)


if __name__ == "__main__":
    construir()
