// Piezas que usan varias páginas: portadas, estrellas y textos seguros.
function escapar(t) {
  return String(t ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

const COLORES_PORTADA = [["#5f3dc4", "#ff6fae"], ["#1864ab", "#ff9e5e"], ["#0b7285", "#ffe066"], ["#862e9c", "#63e6be"], ["#c2255c", "#74c0fc"]];

// Las portadas se piden solo cuando están por entrar en pantalla (bajando o deslizando una fila).
const vigiaPortadas = "IntersectionObserver" in window ? new IntersectionObserver(entradas => {
  for (const e of entradas) {
    if (!e.isIntersecting) continue;
    const img = e.target;
    img.src = img.dataset.src;
    img.removeAttribute("data-src");
    vigiaPortadas.unobserve(img);
  }
}, { rootMargin: "200px 300px" }) : null;

function vigilarPortadas(raiz = document) {
  raiz.querySelectorAll("img[data-src]").forEach(img => {
    if (vigiaPortadas) vigiaPortadas.observe(img);
    else { img.src = img.dataset.src; img.removeAttribute("data-src"); }
  });
}

new MutationObserver(cambios => {
  for (const c of cambios) for (const n of c.addedNodes) if (n.nodeType === 1) vigilarPortadas(n.matches?.("img[data-src]") ? n.parentNode : n);
}).observe(document.documentElement, { childList: true, subtree: true });

function rutaPortada(archivo, grande) {
  return "portadas/" + (grande ? "" : "chicas/") + encodeURIComponent(archivo);
}

function imagenDiferida(archivo, alt = "", grande = false) {
  return `<img data-src="${rutaPortada(archivo, grande)}" alt="${escapar(alt)}" class="diferida" onload="this.classList.add('lista')">`;
}

function portadaImagen(p, grande = false) {
  if (p.portada) return imagenDiferida(p.portada, "Portada de " + p.titulo, grande);
  const [a, b] = COLORES_PORTADA[p.id % COLORES_PORTADA.length];
  return `<div class="sin-portada" style="--a:${a};--b:${b}">${escapar(p.titulo)}</div>`;
}

function tarjeta(p, { vistas = [], conMatch = false, grande = false } = {}) {
  const visto = vistas.includes(p.id) ? '<span class="chapa visto">✔ Vista</span>' : "";
  const match = conMatch && p.puntaje ? `<span class="chapa match">${p.puntaje}% para ti</span>` : "";
  const nota = p.votos ? `⭐ ${p.promedio.toFixed(1)}` : "Sin votos";
  return `<a class="tarjeta" href="pelicula.html?id=${p.id}" title="${escapar(p.titulo)}">
      ${portadaImagen(p, grande)}<span class="chapa anio">${p.anio}</span>${visto}${match}
      <div class="pie">${escapar(p.titulo)}<br><small>${escapar(p.tipo)} · ${nota}</small></div></a>`;
}

function estrellasTexto(n) {
  return n ? "★".repeat(n) + "☆".repeat(5 - n) : "";
}

const COLORES_ACENTO = ["var(--c1)", "var(--c2)", "var(--c3)", "var(--c4)", "var(--c5)"];

function parametro(nombre) {
  return new URLSearchParams(location.search).get(nombre);
}

/** Filas con flechas para mover el carrusel. */
function activarFlechas(raiz = document) {
  raiz.querySelectorAll(".carrusel-caja").forEach(c => {
    const pista = c.querySelector(".carrusel");
    c.querySelectorAll(".flecha").forEach(f => f.addEventListener("click", () => {
      pista.scrollBy({ left: (f.classList.contains("der") ? 1 : -1) * pista.clientWidth * 0.8, behavior: "smooth" });
    }));
  });
}
