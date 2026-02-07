#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

// ============================================================================
// UTILIDADES
// ============================================================================

/**
 * Resuelve una referencia de schema ($ref) a su definición
 */
function resolveSchemaRef(ref, definitions) {
    if (!ref || !ref.startsWith('#/definitions/')) {
        return null;
    }
    const defName = ref.split('/').pop();
    return definitions[defName];
}

/**
 * Formatea un schema a string legible
 */
function formatSchemaType(schema, definitions, depth = 0) {
    if (depth > 3) return 'object'; // Evitar recursión infinita

    if (!schema) return 'any';

    // Si es una referencia, resolverla
    if (schema.$ref) {
        const resolved = resolveSchemaRef(schema.$ref, definitions);
        const defName = schema.$ref.split('/').pop();
        if (resolved) {
            return `<strong>${defName}</strong>`;
        }
        return defName;
    }

    // Si es un array
    if (schema.type === 'array' && schema.items) {
        const itemType = formatSchemaType(schema.items, definitions, depth + 1);
        return `array[${itemType}]`;
    }

    // Si es un objeto con propiedades
    if (schema.type === 'object' && schema.properties) {
        const props = Object.keys(schema.properties).slice(0, 3).join(', ');
        return `object{${props}${Object.keys(schema.properties).length > 3 ? '...' : ''}}`;
    }

    // Tipo simple
    return schema.type || 'any';
}

/**
 * Escapa HTML para prevenir XSS
 */
function escapeHtml(text) {
    if (!text) return '';
    return text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

// ============================================================================
// PARSEO DE SWAGGER
// ============================================================================

/**
 * Agrupa endpoints por tags
 */
function groupEndpointsByTag(paths) {
    const grouped = {};

    for (const [pathUrl, pathItem] of Object.entries(paths)) {
        // Iterar sobre cada método HTTP
        for (const [method, operation] of Object.entries(pathItem)) {
            // Ignorar propiedades que no son métodos HTTP
            if (!['get', 'post', 'put', 'delete', 'patch', 'options', 'head'].includes(method)) {
                continue;
            }

            // Obtener tags (usar 'default' si no hay)
            const tags = operation.tags || ['default'];

            for (const tag of tags) {
                if (!grouped[tag]) {
                    grouped[tag] = [];
                }

                grouped[tag].push({
                    method: method.toUpperCase(),
                    path: pathUrl,
                    summary: operation.summary || '',
                    description: operation.description || '',
                    parameters: operation.parameters || [],
                    responses: operation.responses || {}
                });
            }
        }
    }

    return grouped;
}

/**
 * Genera HTML para la tabla de parámetros
 */
function generateParametersTable(parameters, definitions) {
    if (!parameters || parameters.length === 0) {
        return '<p class="no-params">Este endpoint no requiere parámetros.</p>';
    }

    let html = `
        <table class="param-table">
            <thead>
                <tr>
                    <th>Nombre</th>
                    <th>Ubicación</th>
                    <th>Requerido</th>
                    <th>Tipo</th>
                    <th>Descripción</th>
                </tr>
            </thead>
            <tbody>
    `;

    for (const param of parameters) {
        const name = escapeHtml(param.name);
        const location = param.in || 'unknown';
        const required = param.required
            ? '<span class="required">Sí</span>'
            : '<span class="optional">No</span>';

        // Determinar tipo
        let type = 'any';
        if (param.schema) {
            type = formatSchemaType(param.schema, definitions);
        } else if (param.type) {
            type = param.type;
            if (param.format) {
                type += ` (${param.format})`;
            }
        }

        const description = escapeHtml(param.description || '-');

        html += `
                <tr>
                    <td><code>${name}</code></td>
                    <td>${location}</td>
                    <td>${required}</td>
                    <td>${type}</td>
                    <td>${description}</td>
                </tr>
        `;
    }

    html += `
            </tbody>
        </table>
    `;

    return html;
}

/**
 * Genera HTML para una tarjeta de endpoint
 */
function generateEndpointCard(endpoint, definitions) {
    const methodClass = `method-${endpoint.method.toLowerCase()}`;
    const summary = escapeHtml(endpoint.summary) || 'Sin descripción';
    const description = escapeHtml(endpoint.description);
    const parametersHtml = generateParametersTable(endpoint.parameters, definitions);

    return `
        <div class="endpoint-card">
            <div class="endpoint-header">
                <span class="method-badge ${methodClass}">${endpoint.method}</span>
                <span class="endpoint-path">${escapeHtml(endpoint.path)}</span>
            </div>
            ${summary ? `<div class="endpoint-summary">${summary}</div>` : ''}
            ${description ? `<div class="endpoint-description">${description}</div>` : ''}
            <div class="params-section">
                <h4>Parámetros</h4>
                ${parametersHtml}
            </div>
        </div>
    `;
}

/**
 * Genera HTML para una sección de módulo (tag)
 */
function generateModuleSection(tagName, endpoints, definitions) {
    const endpointsHtml = endpoints
        .map(endpoint => generateEndpointCard(endpoint, definitions))
        .join('\n');

    return `
        <section class="module-section" id="${tagName.replace(/\s+/g, '-').toLowerCase()}">
            <h2>${escapeHtml(tagName)} (${endpoints.length} endpoint${endpoints.length !== 1 ? 's' : ''})</h2>
            ${endpointsHtml}
        </section>
    `;
}

/**
 * Genera HTML para una sección de API completa
 */
function generateApiSection(apiData, apiId) {
    const { swagger, info, host, basePath, paths, definitions } = apiData;

    // Agrupar por tags
    const grouped = groupEndpointsByTag(paths);

    // Generar secciones de módulos
    const modulesHtml = Object.entries(grouped)
        .map(([tag, endpoints]) => generateModuleSection(tag, endpoints, definitions || {}))
        .join('\n');

    // Información de la API
    const apiInfo = `
        <div class="api-info">
            <strong>Título:</strong> ${escapeHtml(info.title || 'N/A')}<br>
            <strong>Versión:</strong> ${escapeHtml(info.version || 'N/A')}<br>
            ${host ? `<strong>Host:</strong> ${escapeHtml(host)}<br>` : ''}
            ${basePath ? `<strong>Base Path:</strong> ${escapeHtml(basePath)}<br>` : ''}
            ${info.description ? `<strong>Descripción:</strong> ${escapeHtml(info.description)}` : ''}
        </div>
    `;

    return `
        <section class="api-section" id="${apiId}">
            <h1>${escapeHtml(info.title || 'API sin título')}</h1>
            ${apiInfo}
            ${modulesHtml}
        </section>
    `;
}

/**
 * Genera items para la tabla de contenidos
 */
function generateTocItems(apis) {
    let html = '<ul>';

    for (const api of apis) {
        const { info, paths } = api.data;
        const grouped = groupEndpointsByTag(paths);

        html += `
            <li>
                <a href="#${api.id}">${escapeHtml(info.title || 'API')} (${Object.keys(paths).length} endpoints)</a>
                <ul>
        `;

        for (const [tag, endpoints] of Object.entries(grouped)) {
            const tagId = tag.replace(/\s+/g, '-').toLowerCase();
            html += `
                    <li><a href="#${tagId}" data-count="${endpoints.length}">${escapeHtml(tag)}</a></li>
            `;
        }

        html += `
                </ul>
            </li>
        `;
    }

    html += '</ul>';
    return html;
}

// ============================================================================
// GENERACIÓN FINAL
// ============================================================================

/**
 * Genera el documento HTML completo
 */
function generateHTML(apis, template) {
    // Calcular estadísticas
    const totalEndpoints = apis.reduce((sum, api) => {
        return sum + Object.keys(api.data.paths).length;
    }, 0);

    // Fecha actual
    const now = new Date();
    const dateStr = now.toLocaleDateString('es-ES', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
    });

    // Generar secciones de APIs
    const apiSectionsHtml = apis
        .map(api => generateApiSection(api.data, api.id))
        .join('\n');

    // Generar TOC
    const tocHtml = generateTocItems(apis);

    // Estadísticas
    const statsHtml = `Total: ${totalEndpoints} endpoints en ${apis.length} APIs`;

    // Reemplazar placeholders
    let html = template;
    html = html.replace('{{GENERATION_DATE}}', dateStr);
    html = html.replace('{{TOTAL_ENDPOINTS}}', statsHtml);
    html = html.replace('{{TOC_ITEMS}}', tocHtml);
    html = html.replace('{{API_SECTIONS}}', apiSectionsHtml);

    return html;
}

// ============================================================================
// MAIN
// ============================================================================

function main() {
    console.log('🚀 Generador de Documentación Swagger → HTML\n');

    // Rutas
    const baseDir = __dirname;
    const adminSwaggerPath = path.join(baseDir, '..', 'edu-admin', 'swagger.json');
    const mobileSwaggerPath = path.join(baseDir, '..', 'edu-mobile', 'swagger.json');
    const templatePath = path.join(baseDir, 'template.html');
    const outputPath = path.join(baseDir, '..', 'output', 'edugo-apis-documentation.html');

    // Verificar archivos de entrada
    console.log('📋 Verificando archivos de entrada...');

    if (!fs.existsSync(adminSwaggerPath)) {
        console.error(`❌ Error: No se encontró ${adminSwaggerPath}`);
        process.exit(1);
    }
    console.log(`   ✓ edu-admin/swagger.json`);

    if (!fs.existsSync(mobileSwaggerPath)) {
        console.error(`❌ Error: No se encontró ${mobileSwaggerPath}`);
        process.exit(1);
    }
    console.log(`   ✓ edu-mobile/swagger.json`);

    if (!fs.existsSync(templatePath)) {
        console.error(`❌ Error: No se encontró ${templatePath}`);
        process.exit(1);
    }
    console.log(`   ✓ template.html\n`);

    // Leer archivos
    console.log('📖 Leyendo especificaciones Swagger...');
    let adminSwagger, mobileSwagger;

    try {
        adminSwagger = JSON.parse(fs.readFileSync(adminSwaggerPath, 'utf8'));
        console.log(`   ✓ edu-admin: ${Object.keys(adminSwagger.paths).length} endpoints`);
    } catch (error) {
        console.error(`❌ Error al parsear edu-admin/swagger.json:`, error.message);
        process.exit(1);
    }

    try {
        mobileSwagger = JSON.parse(fs.readFileSync(mobileSwaggerPath, 'utf8'));
        console.log(`   ✓ edu-mobile: ${Object.keys(mobileSwagger.paths).length} endpoints\n`);
    } catch (error) {
        console.error(`❌ Error al parsear edu-mobile/swagger.json:`, error.message);
        process.exit(1);
    }

    // Leer template
    console.log('📄 Leyendo template HTML...');
    const template = fs.readFileSync(templatePath, 'utf8');
    console.log('   ✓ Template cargado\n');

    // Preparar datos
    const apis = [
        { id: 'edu-admin', data: adminSwagger },
        { id: 'edu-mobile', data: mobileSwagger }
    ];

    // Generar HTML
    console.log('⚙️  Generando HTML...');
    const html = generateHTML(apis, template);
    console.log('   ✓ HTML generado\n');

    // Escribir archivo de salida
    console.log('💾 Guardando archivo...');
    fs.writeFileSync(outputPath, html, 'utf8');
    console.log(`   ✓ Guardado en: ${outputPath}\n`);

    // Estadísticas finales
    const fileSize = fs.statSync(outputPath).size;
    const fileSizeKB = (fileSize / 1024).toFixed(2);

    console.log('✅ ¡Generación completada exitosamente!\n');
    console.log('📊 Estadísticas:');
    console.log(`   • APIs procesadas: ${apis.length}`);
    console.log(`   • Endpoints totales: ${Object.keys(adminSwagger.paths).length + Object.keys(mobileSwagger.paths).length}`);
    console.log(`   • Tamaño del archivo: ${fileSizeKB} KB\n`);
    console.log('🌐 Para ver el documento:');
    console.log(`   open ${outputPath}`);
    console.log('\n📄 Para imprimir:');
    console.log('   1. Abre el archivo en tu navegador');
    console.log('   2. Presiona Ctrl+P (Windows/Linux) o Cmd+P (Mac)');
    console.log('   3. Selecciona "Guardar como PDF" o imprime directamente\n');
}

// Ejecutar
if (require.main === module) {
    main();
}

module.exports = { generateHTML, groupEndpointsByTag };
