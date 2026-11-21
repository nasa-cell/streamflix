// Conversa con el servidor Java y explica cualquier error con palabras claras.

// En internet (Render) las portadas y el video se piden directo a Cloudinary: llegan más livianas
// (formato automático) y sin pasar por el servidor. En la PC salen de la carpeta web, sin internet.
const EN_NUBE = location.hostname.endsWith("onrender.com");
function medio(ruta) {
  if (!EN_NUBE) return ruta;
  const base = "https://res.cloudinary.com/d0em5ykz/";
  const video = ruta.match(/^video\/(.+)\.(mp4|webm)$/);
  if (video) return base + "video/upload/streamflix/video/" + video[1] + "-" + video[2] + "." + video[2];
  return base + "image/upload/f_auto,q_auto/streamflix/" + ruta;
}

class ErrorApi extends Error {
  constructor(mensaje, campo, codigo, detalle, datos = {}) {
    super(mensaje);
    this.datos = datos;
    this.campo = campo;
    this.codigo = codigo;
    this.detalle = detalle;
  }
}

async function api(ruta, datos) {
  const opciones = { headers: {}, credentials: "same-origin" };
  if (datos !== undefined) {
    opciones.method = "POST";
    opciones.headers["Content-Type"] = "application/json";
    opciones.body = JSON.stringify(datos);
  }
  let respuesta;
  try {
    respuesta = await fetch("/api" + ruta, opciones);
  } catch (e) {
    throw new ErrorApi("No hay conexión con el servidor.", null, 0,
      "¿Se cerró la ventana negra de iniciar.bat? Ábrela de nuevo y recarga la página.");
  }
  let cuerpo;
  try {
    cuerpo = await respuesta.json();
  } catch (e) {
    throw new ErrorApi("El servidor respondió algo que no se entiende (código " + respuesta.status + ").", null, respuesta.status, "");
  }
  if (!respuesta.ok || cuerpo.ok === false) {
    if (respuesta.status === 401 && !location.pathname.match(/ingresar|registro|entrada/)) {
      sessionStorage.setItem("sf_mensaje", cuerpo.error || "Vuelve a ingresar.");
      location.href = "ingresar.html";
    }
    if (respuesta.status === 428) {
      sessionStorage.setItem("sf_mensaje", cuerpo.error || "Elige un perfil.");
      location.href = "perfiles.html";
    }
    throw new ErrorApi(cuerpo.error || "Algo salió mal.", cuerpo.campo, respuesta.status, cuerpo.detalle || "", cuerpo);
  }
  return cuerpo;
}

/** Muestra el error en un aviso; si el error es de un campo, lo marca en rojo. */
function mostrarError(e, formulario) {
  const mensaje = e instanceof ErrorApi ? e.message : "Error inesperado en la página.";
  const detalle = e instanceof ErrorApi ? e.detalle : String(e && e.message || e);
  Avisos.mostrar(mensaje, "error", detalle);
  if (formulario && e.campo) {
    const campo = formulario.querySelector(`[name="${e.campo}"]`);
    if (campo) Campos.marcar(campo, false, mensaje);
  }
  console.error(e);
}

// Mensaje que dejó otra página (por ejemplo, "Tu sesión terminó").
document.addEventListener("DOMContentLoaded", () => {
  const m = sessionStorage.getItem("sf_mensaje");
  if (m) { sessionStorage.removeItem("sf_mensaje"); setTimeout(() => Avisos.mostrar(m, "info"), 300); }
});

// En pantallas angostas usa el ejemplo corto si el largo no entra en el campo.
document.addEventListener("DOMContentLoaded", () => {
  if (innerWidth >= 600) return;
  document.querySelectorAll("input[data-corto]").forEach(i => { i.placeholder = i.dataset.corto; });
});
