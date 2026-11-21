(() => {
  Menu.pintar();
  const q = document.getElementById("q");
  const anio = document.getElementById("anio");
  const tipo = document.getElementById("tipo");
  const genero = document.getElementById("genero");
  const salida = document.getElementById("resultados");
  let espera = null;
  let primeraVez = true;
  q.value = parametro("q") || "";

  async function buscar() {
    const ruta = `/peliculas/buscar?q=${encodeURIComponent(q.value)}&anio=${anio.value}&tipo=${encodeURIComponent(tipo.value)}&genero=${encodeURIComponent(genero.value)}`;
    try {
      const r = await api(ruta);
      if (primeraVez) {
        genero.innerHTML += r.generos.map(g => `<option>${escapar(g)}</option>`).join("");
        primeraVez = false;
      }
      document.getElementById("cuantos").textContent = r.resultados.length === 1 ? "1 resultado" : `${r.resultados.length} resultados`;
      salida.innerHTML = r.resultados.length
        ? r.resultados.map(p => tarjeta(p)).join("")
        : `<div class="vacio" style="grid-column:1/-1"><b>🔎</b>No encontramos «${escapar(q.value)}» con esos filtros.<br><small>Prueba con otra palabra o quita un filtro.</small></div>`;
    } catch (e) { mostrarError(e); }
  }

  document.getElementById("buscador").addEventListener("submit", e => { e.preventDefault(); buscar(); });
  q.addEventListener("input", () => { clearTimeout(espera); espera = setTimeout(buscar, 250); });
  [anio, tipo, genero].forEach(s => s.addEventListener("change", buscar));
  buscar();
  q.focus();
})();
