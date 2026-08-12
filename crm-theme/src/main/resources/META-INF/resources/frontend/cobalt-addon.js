// Poison #1: frontend resources of a jar are processed by the Vaadin frontend build.
// If this file reached the preview, the build would try to resolve a package that does not exist.
// FlowThemeJarBuilder must NOT repack META-INF/resources/frontend.
import 'cobalt-nonexistent-npm-package/index.js';

console.log('cobalt add-on frontend module loaded');
