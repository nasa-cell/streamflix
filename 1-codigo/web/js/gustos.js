(async () => {
  Menu.pintar();
  const contenido = document.getElementById("contenido");
  const FUENTES = { estrellas: ["⭐", "Estrellas que diste"], vista: ["👁", "Lo que viste sin calificar"], comentario: ["💬", "Comentarios que leyó (positivo o negativo)"] };

  try {
    const d = await api("/recomendaciones/mis-gustos");
    const p = d.perfil;
    const r = d.resumen;
    contenido.innerHTML = `
      <h1 class="titulo-seccion aparece" style="--c:var(--c1)"><span class="avatar-mini" style="display:inline-grid;background:${p.color};margin-right:8px">${p.avatar}</span>Los gustos de ${escapar(p.nombre)}<small>así te conoce la IA</small></h1>
      <div class="rejilla-4 aparece">
        <div class="dato-grande" style="--c:var(--c1)"><b>${r.vistas}</b><span>películas y series vistas</span></div>
        <div class="dato-grande" style="--c:var(--c2)"><b>${r.calificadas}</b><span>calificadas con estrellas</span></div>
        <div class="dato-grande" style="--c:var(--c3)"><b>${r.promedio || "–"}</b><span>qué tanto te gusta lo que ves (1 a 5)</span></div>
        <div class="dato-grande" style="--c:var(--c4)"><b>${r.horas}</b><span>horas vistas</span></div>
      </div>

      <div class="rejilla-2" style="margin-top:20px">
        <section class="panel aparece">
          <h2>🎯 Tus géneros favoritos</h2>
          <p class="sub">Sale de tus estrellas, lo que viste, tus comentarios y los gustos que elegiste (${escapar(p.gustos.join(", "))}).</p>
          <div id="barras">${Object.entries(d.generos).map(([g, v], i) => `
            <div class="barra-gusto"><span>${escapar(g)}</span><div class="pista"><div class="relleno" data-v="${v}" style="background:${COLORES_ACENTO[i % 5]}"></div></div><span>${v}%</span></div>`).join("") || '<p class="vacio">Aún no hay datos.</p>'}</div>
          <h3 style="margin-top:18px">📚 De dónde aprendió la IA</h3>
          ${Object.entries(d.fuentes).map(([f, n]) => `<div class="vecino"><span>${FUENTES[f][0]}</span>${FUENTES[f][1]}<b>${n}</b></div>`).join("")}
        </section>

        <section class="panel aparece">
          <h2>👥 Perfiles parecidos a ti</h2>
          <p class="sub">Filtrado colaborativo: personas que califican parecido a ti. Lo que les encanta a ellos, te lo recomendamos.</p>
          ${d.vecinos.map(v => `<div class="vecino"><span>🧑</span>${escapar(v.nombre)} <small style="color:var(--suave)">(${v.enComun} en común)</small><b>${v.parecido}%</b></div>`).join("") || '<p class="vacio">Califica algunas películas para encontrar perfiles parecidos.</p>'}
        </section>
      </div>

      <section class="panel aparece" style="margin-top:20px">
        <h2>💡 ¿Por qué te recomendamos esto?</h2>
        <p class="sub">Cada recomendación junta tres técnicas: perfiles parecidos (colaborativo), géneros (contenido) y tu árbol de decisión.</p>
        <div class="rejilla-2">${d.porque.map((x, i) => `
          <a class="porque" href="pelicula.html?id=${x.id}" style="--c:${COLORES_ACENTO[i % 5]}">
            ${x.portada ? imagenDiferida(x.portada) : '<div class="sin-img"></div>'}
            <div><b>${escapar(x.titulo)}</b> <small>· ${x.puntaje}% para ti</small><br><small>${escapar(x.motivo)}</small>
              <div class="partes"><span>👥 ${x.colaborativo}%</span><span>🎭 ${x.contenido}%</span><span>🌳 ${x.arbol}%</span></div></div>
          </a>`).join("")}</div>
      </section>

      <div class="rejilla-2" style="margin-top:20px">
        <section class="panel aparece">
          <h2>🌳 Tu árbol de decisión</h2>
          <p class="sub">${d.nodos} nodos, ${d.profundidad} niveles. Se construye con una función recursiva que elige la mejor pregunta en cada paso.</p>
          <div class="arbol-texto">${escapar(d.arbolTexto)}</div>
        </section>
        <section class="panel aparece">
          <h2>⚖️ Qué pesa más en tus gustos</h2>
          <p class="sub">Importancia de cada característica según tu árbol.</p>
          ${Object.entries(d.importancia).map(([k, v], i) => `
            <div class="barra-gusto"><span>${escapar(k)}</span><div class="pista"><div class="relleno" data-v="${v}" style="background:${COLORES_ACENTO[(i + 2) % 5]}"></div></div><span>${v}%</span></div>`).join("")}
          <h3 style="margin-top:18px">💬 Lo que la IA entendió de tus comentarios</h3>
          ${d.sentimientos.length ? d.sentimientos.map(s => `<div class="vecino" style="align-items:flex-start"><span class="sentimiento ${s.etiqueta}">${s.etiqueta}</span><span><b style="color:var(--texto);margin:0">${escapar(s.pelicula)}</b><br><small style="color:var(--suave)">«${escapar(s.texto)}»</small></span></div>`).join("") : '<p class="sub">Aún no comentas. Tus comentarios también mejoran tus recomendaciones.</p>'}
        </section>
      </div>

      <section class="panel acento aparece" style="margin-top:20px" id="encuesta">
        <h2>📝 Encuesta de satisfacción</h2>
        <p class="sub">Nos ayuda a medir si el sistema nuevo funciona mejor. Toca un número del 1 (nada) al 5 (mucho).</p>
        <div class="rejilla-3">
          <div class="campo"><label>¿Qué tan satisfecho estás? <span class="marca obligatorio">1 A 5</span></label><div class="escala" data-p="satisfaccion"></div></div>
          <div class="campo"><label>¿Acertamos con tus gustos? <span class="marca obligatorio">1 A 5</span></label><div class="escala" data-p="acierto"></div></div>
          <div class="campo"><label>¿Es fácil de usar? <span class="marca obligatorio">1 A 5</span></label><div class="escala" data-p="facilidad"></div></div>
        </div>
        <div class="campos" style="margin-top:14px">
          <div class="campo"><label>¿Recomendarías StreamFlix? <span class="marca obligatorio">ELIGE</span></label>
            <select class="entrada" id="recomendaria"><option value="">Elige una opción</option><option value="si">Sí</option><option value="no">No</option></select></div>
          <div class="campo"><label for="comentario-encuesta">Comentario <span class="marca opcional">OPCIONAL</span></label>
            <input class="entrada" id="comentario-encuesta" maxlength="200" placeholder="Ejemplo: más anime, por favor"></div>
        </div>
        <div class="fila-botones"><button class="btn btn-1" id="enviar-encuesta">Enviar encuesta</button></div>
      </section>`;

    setTimeout(() => document.querySelectorAll(".relleno").forEach(b => { b.style.width = b.dataset.v + "%"; }), 80);
    prepararEncuesta();
  } catch (e) {
    mostrarError(e);
    contenido.innerHTML = `<div class="vacio"><b>😕</b>${escapar(e.message)}</div>`;
  }

  function prepararEncuesta() {
    const respuestas = {};
    document.querySelectorAll(".escala").forEach(c => {
      c.innerHTML = [1, 2, 3, 4, 5].map(n => `<button type="button" data-n="${n}">${n}</button>`).join("");
      c.addEventListener("click", e => {
        const b = e.target.closest("button");
        if (!b) return;
        respuestas[c.dataset.p] = Number(b.dataset.n);
        c.querySelectorAll("button").forEach(x => x.classList.toggle("si", x === b));
      });
    });
    document.getElementById("enviar-encuesta").addEventListener("click", async () => {
      const faltan = ["satisfaccion", "acierto", "facilidad"].filter(k => !respuestas[k]);
      const rec = document.getElementById("recomendaria").value;
      if (faltan.length || !rec) {
        Avisos.mostrar("Faltan respuestas en la encuesta.", "error", "Toca un número en cada pregunta y elige si nos recomendarías.");
        return;
      }
      try {
        const r = await api("/interaccion/encuesta", { ...respuestas, recomendaria: rec === "si", comentario: document.getElementById("comentario-encuesta").value });
        Avisos.mostrar(r.mensaje, "ok", `Promedio de satisfacción: ${r.resumen.satisfaccion} de 5 (${r.resumen.respuestas} respuestas).`);
        document.getElementById("enviar-encuesta").disabled = true;
      } catch (e) { mostrarError(e); }
    });
  }
})();
