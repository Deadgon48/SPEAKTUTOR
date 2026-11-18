# Paper Shader Ticket — vanilla HTML/CSS/JS

A browser-native conversion of the supplied React/TypeScript ticket component. No build step or dependencies are needed. Serve this directory over HTTP (for example, `python -m http.server 8080`) and open `index.html` in a WebGL 2-capable browser.

## Files
- `index.html` — demo page and markup.
- `styles.css` — responsive page, ticket and canvas styling.
- `app.js` — WebGL shader runtime, procedural and image dithering renderers, color/image helpers, ticket geometry/layout helpers, tilt effect and demo initialization.

## Browser API
`window.PaperShaders` exports `ShaderMount`, `mountDithering(element, options)`, `mountImageDithering(element, options)`, `createTicket(container, options)`, color/image processing helpers, ticket geometry/layout/texture presets and shader sources. Each mount/ticket returns a disposable instance. `createTicket` accepts the ticket fields (`name`, `presenter`, `event`, `venue`, `dates`, `stubText`, `watermark`) and configuration (`width`, `geometry`, `layout`, `texture`, `gradient`, `tilt`). For example:

```js
const ticket = PaperShaders.createTicket(document.querySelector('#ticket'), {
  name: 'ALEX MORGAN',
  presenter: 'NIGHT SHIFT',
  event: 'LIVE SESSIONS',
  venue: 'THE GRAND HALL',
  dates: 'FRI · 8 PM',
  stubText: 'ADMIT ONE'
});
// Later: ticket.dispose();
```

The original React hooks/ref lifecycle were adapted to explicit DOM mount/dispose calls; JSX is represented by DOM creation, and the GLSL rendering still requires WebGL 2. The standalone demo uses the procedural shader by default.
