// Reglas de los formularios: dónde van letras, dónde van números, mínimos,
// máximos, contador y aviso en vivo si un dato ya está registrado.
const Campos = (() => {
  const SOLO = {
    letras: { quitar: /[^A-Za-zÁÉÍÓÚáéíóúÑñÜü ]/g, aviso: "Aquí solo van letras y espacios." },
    numeros: { quitar: /[^0-9]/g, aviso: "Aquí solo van números." },
    alfanum: { quitar: /[^A-Za-z0-9]/g, aviso: "Solo letras y números, sin espacios ni símbolos." },
    clave: { quitar: /\s/g, aviso: "La contraseña no lleva espacios." },
    perfil: { quitar: /[^A-Za-z0-9ÁÉÍÓÚáéíóúÑñ]/g, aviso: "Solo letras y números, sin espacios." },
    correo: { quitar: /[^A-Za-z0-9.@]/g, aviso: "En el Gmail solo van letras, números, punto y @." },
  };
  const temporizadores = new WeakMap();

  function ayuda(input) {
    return input.closest(".campo").querySelector(".ayuda span:first-child");
  }

  function marcar(input, bien, mensaje) {
    const campo = input.closest(".campo");
    const span = ayuda(input);
    if (!span.dataset.original) span.dataset.original = span.textContent;
    campo.classList.toggle("bien", bien === true);
    campo.classList.toggle("mal", bien === false);
    span.classList.remove("aviso-tecla");
    span.textContent = mensaje || span.dataset.original;
  }

  function avisoTecla(input, texto) {
    const span = ayuda(input);
    if (!span.dataset.original) span.dataset.original = span.textContent;
    span.textContent = "✋ " + texto;
    span.classList.add("aviso-tecla");
    input.closest(".campo").classList.remove("mal");
    void input.offsetWidth;
    input.closest(".campo").classList.add("mal");
    Sonido.teclaMala();
    clearTimeout(temporizadores.get(input));
    temporizadores.set(input, setTimeout(() => revisar(input, false), 1800));
  }

  // Lista de condiciones de la contraseña que se ponen en verde mientras se escribe.
  const CONDICIONES = [v => v.length >= 8 && v.length <= 20, v => /[A-Za-z]/.test(v), v => /[0-9]/.test(v), v => /[^A-Za-z0-9\s]/.test(v)];
  function listaClave(input) {
    const lista = input.closest(".campo").querySelector(".reglas-clave");
    if (!lista) return;
    lista.querySelectorAll("li").forEach((li, i) => {
      const ok = CONDICIONES[i](input.value);
      li.classList.toggle("ok", ok);
      li.querySelector("b").textContent = ok ? "✔" : "○";
    });
  }

  function contador(input) {
    const c = input.closest(".campo").querySelector(".contador");
    if (c && input.maxLength > 0) c.textContent = `${input.value.length}/${input.maxLength}`;
  }

  function revisar(input, mostrarVacio = true) {
    const v = input.value.trim();
    contador(input);
    listaClave(input);
    if (!v) {
      marcar(input, mostrarVacio && input.required ? false : null, mostrarVacio && input.required ? "Este dato es obligatorio." : null);
      return !input.required;
    }
    if (input.dataset.min && v.length < +input.dataset.min) {
      marcar(input, false, `Muy corto: mínimo ${input.dataset.min} (llevas ${v.length}).`);
      return false;
    }
    if (input.dataset.regla && !new RegExp(input.dataset.regla).test(v)) {
      marcar(input, false, input.dataset.mensaje || "Revisa este dato.");
      return false;
    }
    if (input.dataset.rango) {
      const [a, b] = input.dataset.rango.split("-").map(Number);
      if (+v < a || +v > b) { marcar(input, false, `Debe estar entre ${a} y ${b}.`); return false; }
    }
    if (input.dataset.ocupado === v.toLowerCase()) {
      marcar(input, false, input.dataset.mensajeOcupado);
      return false;
    }
    marcar(input, true, input.dataset.unico && input.dataset.libre === v.toLowerCase() ? "✔ Disponible." : "✔ Correcto.");
    return true;
  }

  async function revisarUnico(input) {
    const v = input.value.trim();
    if (!revisar(input, false) || !input.dataset.unico) return;
    try {
      const r = await api(`/cuenta/disponible?campo=${input.dataset.unico}&valor=${encodeURIComponent(v)}`);
      if (input.value.trim() !== v) return;
      if (r.disponible) {
        input.dataset.libre = v.toLowerCase();
        input.dataset.ocupado = "";
        marcar(input, true, "✔ Disponible.");
      } else {
        input.dataset.ocupado = v.toLowerCase();
        input.dataset.mensajeOcupado = input.dataset.textoRepetido || "Ya está registrado. Usa otro.";
        marcar(input, false, input.dataset.mensajeOcupado);
        Sonido.error();
      }
    } catch (e) { /* si falla la revisión en vivo, el servidor vuelve a revisar al guardar */ }
  }

  function preparar(formulario) {
    formulario.querySelectorAll("input, textarea").forEach(input => {
      contador(input);
      input.addEventListener("input", () => {
        const tipo = SOLO[input.dataset.solo];
        if (tipo) {
          const limpio = input.value.replace(tipo.quitar, "");
          if (limpio !== input.value) {
            const pos = input.selectionStart - (input.value.length - limpio.length);
            input.value = limpio;
            try { input.setSelectionRange(pos, pos); } catch (e) { /* algunos tipos no lo permiten */ }
            contador(input);
            listaClave(input);
            avisoTecla(input, tipo.aviso);
            return;
          }
        }
        revisar(input, false);
        if (input.dataset.unico) {
          clearTimeout(temporizadores.get(input));
          temporizadores.set(input, setTimeout(() => revisarUnico(input), 450));
        }
      });
      input.addEventListener("blur", () => { if (input.value) revisar(input); });
    });
  }

  function validarTodo(formulario) {
    let primero = null;
    formulario.querySelectorAll("input[required], input[data-regla], textarea[required]").forEach(i => {
      if (!revisar(i) && !primero) primero = i;
    });
    if (primero) {
      primero.focus();
      Avisos.mostrar("Revisa los datos marcados en rojo.", "error", ayuda(primero).textContent);
      return false;
    }
    return true;
  }

  function datos(formulario) {
    const d = {};
    formulario.querySelectorAll("[name]").forEach(i => { d[i.name] = i.value.trim(); });
    return d;
  }

  return { preparar, validarTodo, datos, marcar };
})();

// Botón 👁 para ver u ocultar la contraseña.
document.addEventListener("click", e => {
  const b = e.target.closest(".ver-clave");
  if (!b) return;
  const i = b.parentElement.querySelector("input");
  i.type = i.type === "password" ? "text" : "password";
  b.textContent = i.type === "password" ? "👁" : "🙈";
});
