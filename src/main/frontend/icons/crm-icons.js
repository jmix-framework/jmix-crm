import '@vaadin/icon/vaadin-iconset.js';

const template = document.createElement('template');
template.innerHTML = `
<vaadin-iconset name="crm" size="24">
  <svg xmlns="http://www.w3.org/2000/svg">
    <defs>
      <g id="crm:sparkles" fill="none" stroke="currentColor" stroke-width="1.8"
         stroke-linecap="round" stroke-linejoin="round">
        <path d="M12 4v4 M12 16v4 M4 12h4 M16 12h4
                 M6 6l2.5 2.5 M15.5 15.5 18 18
                 M6 18l2.5-2.5 M15.5 8.5 18 6"/>
      </g>
    </defs>
  </svg>
</vaadin-iconset>
`;
document.head.appendChild(template.content);
