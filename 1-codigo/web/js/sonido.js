// Sonidos de StreamFlix: música de entrada y efectos de botones, creados con
// Web Audio (no usan canciones con derechos). Se pueden apagar con 🔊 / 🔇.
const Sonido = (() => {
  let ctx = null;
  let activo = true;
  try { activo = localStorage.getItem("sf_sonido") !== "no"; } catch (e) { /* sin almacenamiento */ }

  function contexto() {
    if (!ctx) {
      const C = window.AudioContext || window.webkitAudioContext;
      if (!C) return null;
      ctx = new C();
    }
    if (ctx.state === "suspended") ctx.resume();
    return ctx;
  }

  function nota(frec, inicio, dur, { tipo = "sine", vol = 0.12, ataque = 0.01, destino = null } = {}) {
    const a = contexto();
    if (!a) return;
    const t = a.currentTime + inicio;
    const o = a.createOscillator();
    const g = a.createGain();
    o.type = tipo;
    o.frequency.setValueAtTime(frec, t);
    g.gain.setValueAtTime(0.0001, t);
    g.gain.exponentialRampToValueAtTime(vol, t + ataque);
    g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
    o.connect(g).connect(destino || a.destination);
    o.start(t);
    o.stop(t + dur + 0.05);
  }

  function barrido(desde, hasta, inicio, dur, vol = 0.08) {
    const a = contexto();
    if (!a) return;
    const t = a.currentTime + inicio;
    const o = a.createOscillator();
    const g = a.createGain();
    o.type = "sawtooth";
    o.frequency.setValueAtTime(desde, t);
    o.frequency.exponentialRampToValueAtTime(hasta, t + dur);
    const f = a.createBiquadFilter();
    f.type = "lowpass";
    f.frequency.setValueAtTime(800, t);
    f.frequency.linearRampToValueAtTime(4000, t + dur);
    g.gain.setValueAtTime(0.0001, t);
    g.gain.exponentialRampToValueAtTime(vol, t + dur * 0.7);
    g.gain.exponentialRampToValueAtTime(0.0001, t + dur + 0.2);
    o.connect(f).connect(g).connect(a.destination);
    o.start(t);
    o.stop(t + dur + 0.3);
  }

  return {
    get activo() { return activo; },
    cambiar() {
      activo = !activo;
      try { localStorage.setItem("sf_sonido", activo ? "si" : "no"); } catch (e) { /* sin almacenamiento */ }
      if (activo) this.clic();
      return activo;
    },
    clic() { if (activo) { nota(660, 0, 0.08, { tipo: "triangle", vol: 0.07 }); nota(990, 0.03, 0.06, { tipo: "sine", vol: 0.04 }); } },
    suave() { if (activo) nota(520, 0, 0.07, { tipo: "sine", vol: 0.05 }); },
    exito() { if (activo) [523.3, 659.3, 784].forEach((f, i) => nota(f, i * 0.08, 0.35, { tipo: "triangle", vol: 0.09 })); },
    error() { if (activo) { nota(330, 0, 0.18, { tipo: "square", vol: 0.05 }); nota(247, 0.15, 0.28, { tipo: "square", vol: 0.05 }); } },
    aviso() { if (activo) { nota(880, 0, 0.15, { tipo: "sine", vol: 0.08 }); nota(660, 0.14, 0.2, { tipo: "sine", vol: 0.08 }); } },
    estrella(n) { if (activo) for (let i = 0; i < n; i++) nota(700 + i * 120, i * 0.06, 0.18, { tipo: "triangle", vol: 0.07 }); },
    teclaMala() { if (activo) nota(200, 0, 0.08, { tipo: "square", vol: 0.04 }); },

    // Música de la entrada (unos 3,5 s), sincronizada con la animación del botón play.
    musicaEntrada() {
      if (!activo || !contexto()) return;
      const pad = [261.6, 329.6, 392];
      pad.forEach(f => nota(f / 2, 0, 3.4, { tipo: "sine", vol: 0.05, ataque: 0.6 }));
      [392, 523.3, 659.3, 784].forEach((f, i) => nota(f, 0.15 + i * 0.22, 0.4, { tipo: "triangle", vol: 0.08 }));
      barrido(120, 1200, 1.1, 0.9, 0.05);
      nota(65.4, 2.0, 1.2, { tipo: "sine", vol: 0.25, ataque: 0.005 });
      [523.3, 659.3, 784, 1046.5].forEach(f => nota(f, 2.0, 1.6, { tipo: "triangle", vol: 0.06, ataque: 0.02 }));
      "StreamFlix".split("").forEach((_, i) => nota(1046.5 + i * 60, 2.15 + i * 0.06, 0.12, { tipo: "sine", vol: 0.03 }));
      nota(1568, 2.9, 0.9, { tipo: "sine", vol: 0.05 });
    },
  };
})();

// Clic suave en todos los botones y enlaces con forma de botón.
document.addEventListener("pointerdown", e => {
  const b = e.target.closest("button, .btn, .tarjeta, .chip, .enlaces a, .perfil");
  if (!b || b.dataset.sinSonido !== undefined || b.disabled) return;
  if (b.classList.contains("tarjeta") || b.classList.contains("chip")) Sonido.suave();
  else Sonido.clic();
});
