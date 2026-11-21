package streamflix.servicio;

/**
 * Diseño del correo con el código para cambiar la contraseña.
 * Usa tablas y estilos dentro de cada etiqueta porque Gmail ignora las hojas de estilo.
 */
final class CorreoCodigo {

    private CorreoCodigo() {}

    static String asunto(String codigo) {
        return "Tu código de StreamFlix: " + codigo;
    }

    static String texto(String nombre, String codigo, int minutos) {
        return "Hola, " + nombre + ".\n\nPediste cambiar tu contraseña de StreamFlix. Tu código es: " + codigo
                + "\n\nVale " + minutos + " minutos y tienes 3 intentos.\n\n"
                + "¿No fuiste tú? Ignora este correo: tu contraseña no cambia y tu cuenta sigue segura. Nunca compartas este código.\n\n"
                + "StreamFlix · Sistema de recomendación · PIAD-428";
    }

    static String html(String nombre, String codigo, int minutos) {
        return """
            <div style="background:#f3f1fa;padding:24px 12px">
            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:520px;margin:0 auto;font-family:Arial,Helvetica,sans-serif;border-radius:16px;overflow:hidden;border:1px solid #e3e0f0;background:#ffffff">
              <tr><td style="background:#141433;padding:22px 24px;text-align:center">
                <div style="font-size:28px;font-weight:800;letter-spacing:1px"><span style="color:#ff6fae">Stream</span><span style="color:#ffd84d">F</span><span style="color:#5fe0b7">l</span><span style="color:#6cb8ff">i</span><span style="color:#ff9f5a">x</span></div>
                <div style="color:#c9c7ec;font-size:13px;margin-top:4px">Recuperar contraseña</div>
              </td></tr>
              <tr><td style="height:5px;line-height:5px;font-size:0;background:#ff6fae;background-image:linear-gradient(90deg,#ff6fae,#ff9f5a,#ffd84d,#5fe0b7,#6cb8ff)">&nbsp;</td></tr>
              <tr><td style="padding:26px 24px;color:#22223a">
                <p style="margin:0 0 6px;font-size:17px"><b>Hola, {nombre} 👋</b></p>
                <p style="margin:0 0 18px;color:#5b5b78;font-size:14px;line-height:1.5">Pediste cambiar tu contraseña. Escribe este código en la página de StreamFlix:</p>
                <div style="text-align:center;margin:0 0 18px">
                  <span style="display:inline-block;background:#fff0f6;border:2px dashed #ff6fae;border-radius:14px;padding:12px 22px;font-size:34px;font-weight:800;letter-spacing:10px;color:#141433">{codigo}</span>
                </div>
                <p style="margin:0;text-align:center;font-size:13px;color:#5b5b78">⏱️ Vale <b>{minutos} minutos</b> · 3 intentos</p>
                <div style="margin-top:20px;background:#effcf7;border-left:4px solid #5fe0b7;border-radius:0 10px 10px 0;padding:10px 14px;font-size:13px;line-height:1.5;color:#22223a">
                  🔒 <b>¿No fuiste tú?</b> Ignora este correo: tu contraseña no cambia y tu cuenta sigue segura. Nunca compartas este código.
                </div>
              </td></tr>
              <tr><td style="background:#f7f5fc;padding:14px 24px;text-align:center;font-size:11px;line-height:1.5;color:#8a88a8">StreamFlix · Sistema de recomendación · PIAD-428<br>Correo automático, no respondas a este mensaje.</td></tr>
            </table>
            </div>""".replace("{nombre}", escapar(nombre)).replace("{codigo}", codigo).replace("{minutos}", String.valueOf(minutos));
    }

    private static String escapar(String t) {
        return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
