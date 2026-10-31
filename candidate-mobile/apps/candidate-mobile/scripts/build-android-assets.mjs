#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const ts = require('typescript');

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const APP_DIR = path.resolve(__dirname, '..');
const WORKSPACE_DIR = path.resolve(APP_DIR, '../..');
const CORE_DIR = path.resolve(WORKSPACE_DIR, 'packages/candidate-core');
const CORE_SRC_DIR = path.resolve(CORE_DIR, 'src');
const APP_SRC_DIR = path.resolve(APP_DIR, 'src');
const ASSETS_DIR = path.resolve(APP_DIR, 'android/app/src/main/assets/www');

console.log('[NAG Assets Builder] Initializing bundle build...');
console.log(`[NAG Assets Builder] App Dir: ${APP_DIR}`);
console.log(`[NAG Assets Builder] Target Assets Dir: ${ASSETS_DIR}`);

// Ensure output directories exist
fs.mkdirSync(ASSETS_DIR, { recursive: true });

function collectSourceFiles(dir) {
  const result = [];
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (entry.name !== '__tests__') {
        result.push(...collectSourceFiles(fullPath));
      }
    } else if (
      (entry.name.endsWith('.ts') || entry.name.endsWith('.tsx')) &&
      !entry.name.endsWith('.d.ts')
    ) {
      result.push(fullPath);
    }
  }
  return result;
}

const allSourceFiles = [
  ...collectSourceFiles(CORE_SRC_DIR),
  ...collectSourceFiles(APP_SRC_DIR),
];

console.log(`[NAG Assets Builder] Discovered ${allSourceFiles.length} source files.`);

// Normalize module paths to unique keys
function getModuleId(filePath) {
  if (filePath.startsWith(CORE_SRC_DIR)) {
    const rel = path.relative(CORE_SRC_DIR, filePath).replace(/\\/g, '/');
    return `@core/${rel}`;
  }
  if (filePath.startsWith(APP_SRC_DIR)) {
    const rel = path.relative(APP_SRC_DIR, filePath).replace(/\\/g, '/');
    return `@app/${rel}`;
  }
  return filePath.replace(/\\/g, '/');
}

function resolveImport(fromFilePath, importPath) {
  if (
    importPath === 'react' ||
    importPath === 'react/jsx-runtime' ||
    importPath === 'react-native' ||
    importPath === 'crypto' ||
    importPath.startsWith('expo-')
  ) {
    return { type: 'builtin', name: importPath };
  }

  if (importPath === '@nag/candidate-core') {
    return { type: 'file', id: getModuleId(path.join(CORE_SRC_DIR, 'index.ts')) };
  }

  if (importPath.startsWith('@nag/candidate-core/')) {
    const sub = importPath.slice('@nag/candidate-core/'.length);
    const candidate1 = path.join(CORE_SRC_DIR, `${sub}.ts`);
    if (fs.existsSync(candidate1)) return { type: 'file', id: getModuleId(candidate1) };
    const candidate2 = path.join(CORE_SRC_DIR, sub, 'index.ts');
    if (fs.existsSync(candidate2)) return { type: 'file', id: getModuleId(candidate2) };
  }

  if (importPath.startsWith('.')) {
    const dir = path.dirname(fromFilePath);
    const candidatePaths = [
      path.resolve(dir, `${importPath}.tsx`),
      path.resolve(dir, `${importPath}.ts`),
      path.resolve(dir, importPath, 'index.tsx'),
      path.resolve(dir, importPath, 'index.ts'),
    ];
    for (const cp of candidatePaths) {
      if (fs.existsSync(cp)) return { type: 'file', id: getModuleId(cp) };
    }
  }

  throw new Error(`Unable to resolve import "${importPath}" from "${fromFilePath}"`);
}

const transpiledModules = {};

for (const filePath of allSourceFiles) {
  const moduleId = getModuleId(filePath);
  const sourceCode = fs.readFileSync(filePath, 'utf8');

  const transpiled = ts.transpileModule(sourceCode, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022,
      jsx: ts.JsxEmit.React,
      esModuleInterop: true,
      skipLibCheck: true,
    },
    fileName: filePath,
  });

  const outputText = transpiled.outputText;
  const deps = {};
  const requireRegex = /require\(['"]([^'"]+)['"]\)/g;
  let match;
  while ((match = requireRegex.exec(outputText)) !== null) {
    const imp = match[1];
    const resolved = resolveImport(filePath, imp);
    if (resolved.type === 'builtin') {
      deps[imp] = resolved.name;
    } else {
      deps[imp] = resolved.id;
    }
  }

  transpiledModules[moduleId] = {
    deps,
    code: outputText,
  };
}

console.log(`[NAG Assets Builder] Successfully transpiled ${Object.keys(transpiledModules).length} modules.`);

// Mini-React 18 & Self-contained DOM Renderer Implementation
const runtimeShim = `
/**
 * NAG Official Candidate Mobile - Self-Contained React 18 & DOM Renderer Shim
 * Provides React 18 hooks (useState, useEffect, useMemo, useCallback, useRef, Fragment),
 * JSX element factories (createElement, jsx, jsxs), and virtual DOM reconciler with full
 * SVG support, event delegation, and microtask scheduling.
 */
const NagReact = (function() {
  const Fragment = Symbol.for('react.fragment');

  let currentComponent = null;
  let currentHookIndex = 0;
  let rootContainer = null;
  let rootElement = null;
  let rootVNode = null;
  let isRenderPending = false;
  const pendingEffects = [];

  function isShallowEqual(a, b) {
    if (Object.is(a, b)) return true;
    if (typeof a !== 'object' || a === null || typeof b !== 'object' || b === null) return false;
    if (Array.isArray(a) !== Array.isArray(b)) return false;
    const keysA = Object.keys(a);
    const keysB = Object.keys(b);
    if (keysA.length !== keysB.length) return false;
    for (let i = 0; i < keysA.length; i++) {
      const key = keysA[i];
      if (!Object.prototype.hasOwnProperty.call(b, key) || !Object.is(a[key], b[key])) {
        return false;
      }
    }
    return true;
  }

  function createElement(type, props, ...children) {
    const p = Object.assign({}, props || {});
    const flat = children.flat(Infinity).filter(c => c !== null && c !== undefined && c !== false && c !== true);
    if (flat.length === 1) {
      p.children = flat[0];
    } else if (flat.length > 1) {
      p.children = flat;
    }
    return {
      type,
      props: p,
      key: props && props.key !== undefined ? props.key : null,
    };
  }

  function jsx(type, props, key) {
    const p = Object.assign({}, props || {});
    return {
      type,
      props: p,
      key: key !== undefined ? key : (p.key !== undefined ? p.key : null),
    };
  }
  const jsxs = jsx;

  function useState(initialState) {
    if (!currentComponent) {
      throw new Error('useState called outside of component render');
    }
    const comp = currentComponent;
    const idx = currentHookIndex++;
    if (comp.hooks[idx] === undefined) {
      comp.hooks[idx] = typeof initialState === 'function' ? initialState() : initialState;
    }
    const setState = (next) => {
      const currentVal = comp.hooks[idx];
      const nextVal = typeof next === 'function' ? next(currentVal) : next;
      if (!isShallowEqual(currentVal, nextVal)) {
        comp.hooks[idx] = nextVal;
        scheduleRender();
      }
    };
    return [comp.hooks[idx], setState];
  }

  function useRef(initialValue) {
    if (!currentComponent) {
      throw new Error('useRef called outside of component render');
    }
    const comp = currentComponent;
    const idx = currentHookIndex++;
    if (comp.hooks[idx] === undefined) {
      comp.hooks[idx] = { current: initialValue };
    }
    return comp.hooks[idx];
  }

  function useMemo(factory, deps) {
    if (!currentComponent) {
      throw new Error('useMemo called outside of component render');
    }
    const comp = currentComponent;
    const idx = currentHookIndex++;
    const prev = comp.hooks[idx];
    if (!prev || depsChanged(prev.deps, deps)) {
      const val = factory();
      comp.hooks[idx] = { val, deps };
      return val;
    }
    return prev.val;
  }

  function useCallback(fn, deps) {
    return useMemo(() => fn, deps);
  }

  function useEffect(effect, deps) {
    if (!currentComponent) {
      throw new Error('useEffect called outside of component render');
    }
    const comp = currentComponent;
    const idx = currentHookIndex++;
    const prev = comp.hooks[idx];
    const hasChanged = !prev || depsChanged(prev.deps, deps);
    comp.hooks[idx] = {
      effect,
      deps,
      cleanup: prev ? prev.cleanup : undefined,
      hasChanged,
    };
    if (hasChanged) {
      pendingEffects.push({ comp, hookIdx: idx });
    }
  }

  function depsChanged(oldDeps, newDeps) {
    if (!oldDeps || !newDeps || oldDeps.length !== newDeps.length) return true;
    for (let i = 0; i < oldDeps.length; i++) {
      const a = oldDeps[i];
      const b = newDeps[i];
      if (Object.is(a, b)) continue;
      if (typeof a === 'function' && typeof b === 'function' && a.toString() === b.toString()) continue;
      return true;
    }
    return false;
  }

  function flushEffects() {
    while (pendingEffects.length > 0) {
      const { comp, hookIdx } = pendingEffects.shift();
      const hook = comp.hooks[hookIdx];
      if (hook && hook.hasChanged) {
        if (typeof hook.cleanup === 'function') {
          try { hook.cleanup(); } catch (err) { console.error('[NagReact] Error in cleanup:', err); }
        }
        try {
          hook.cleanup = hook.effect();
        } catch (err) {
          console.error('[NagReact] Error in useEffect:', err);
        }
        hook.hasChanged = false;
      }
    }
  }

  function scheduleRender() {
    if (isRenderPending) return;
    isRenderPending = true;
    setTimeout(() => {
      isRenderPending = false;
      renderRoot();
    }, 0);
  }

  function renderRoot() {
    if (!rootContainer || !rootElement) return;
    const nextVNode = { type: rootElement.type, props: rootElement.props, key: rootElement.key };
    patch(rootContainer, nextVNode, rootVNode, null, false);
    rootVNode = nextVNode;
    flushEffects();
  }

  const SVG_TAGS = new Set([
    'svg', 'path', 'circle', 'rect', 'line', 'polyline', 'polygon',
    'g', 'text', 'defs', 'clipPath', 'mask', 'pattern', 'image', 'use',
    'linearGradient', 'radialGradient', 'stop'
  ]);

  const NON_DIMENSIONAL_PROPS = /acit|ex(?:s|g|n|p|$)|rph|grid|ows|mnc|ontw|oind|st[a-z]|wko/i;

  function flattenChildren(children) {
    if (children == null || typeof children === 'boolean') return [];
    if (!Array.isArray(children)) children = [children];
    const result = [];
    function walk(items) {
      for (let i = 0; i < items.length; i++) {
        const item = items[i];
        if (item == null || typeof item === 'boolean') continue;
        if (Array.isArray(item)) {
          walk(item);
        } else if (typeof item === 'object' && item.type === Fragment) {
          if (item.props && item.props.children) {
            walk(Array.isArray(item.props.children) ? item.props.children : [item.props.children]);
          }
        } else if (typeof item === 'string' || typeof item === 'number') {
          result.push({ type: '#text', text: String(item), props: null, key: null });
        } else {
          result.push(item);
        }
      }
    }
    walk(children);
    return result;
  }

  function updateProps(dom, newProps, oldProps, isSvg) {
    newProps = newProps || {};
    oldProps = oldProps || {};

    for (const key in oldProps) {
      if (key === 'children' || key === 'key') continue;
      if (!(key in newProps)) {
        if (key === 'style') {
          dom.removeAttribute('style');
        } else if (key.startsWith('on')) {
          const event = key.slice(2).toLowerCase();
          if (dom._listeners) delete dom._listeners[event];
        } else if (key === 'className' || key === 'class') {
          dom.removeAttribute('class');
        } else if (key === 'value' || key === 'checked') {
          dom[key] = '';
        } else {
          dom.removeAttribute(key);
        }
      }
    }

    for (const key in newProps) {
      if (key === 'children' || key === 'key') continue;
      const val = newProps[key];
      const oldVal = oldProps[key];
      if (val === oldVal && key !== 'value' && key !== 'checked') continue;

      if (key === 'style') {
        if (typeof val === 'string') {
          dom.style.cssText = val;
        } else if (typeof val === 'object' && val !== null) {
          if (typeof oldVal === 'object' && oldVal !== null) {
            for (const s in oldVal) {
              if (!(s in val)) dom.style[s] = '';
            }
          }
          for (const s in val) {
            let v = val[s];
            if (typeof v === 'number' && v !== 0 && !NON_DIMENSIONAL_PROPS.test(s)) {
              v = v + 'px';
            }
            dom.style[s] = v == null ? '' : v;
          }
        }
      } else if (key.startsWith('on')) {
        const event = key.slice(2).toLowerCase();
        if (!dom._listeners) dom._listeners = {};
        if (!dom._listeners[event]) {
          const listener = (e) => {
            const handler = dom._listeners && dom._listeners[event];
            if (typeof handler === 'function') handler(e);
          };
          dom.addEventListener(event, listener);
          if (event === 'change' && (dom.tagName === 'INPUT' || dom.tagName === 'TEXTAREA')) {
            dom.addEventListener('input', listener);
          }
        }
        dom._listeners[event] = val;
      } else if (key === 'className' || key === 'class') {
        if (isSvg) dom.setAttribute('class', val || '');
        else dom.className = val || '';
      } else if (key === 'value') {
        dom.value = val == null ? '' : val;
      } else if (key === 'checked') {
        dom.checked = Boolean(val);
      } else if (key === 'disabled') {
        dom.disabled = Boolean(val);
        if (val) dom.setAttribute('disabled', '');
        else dom.removeAttribute('disabled');
      } else if (key === 'readOnly') {
        dom.readOnly = Boolean(val);
      } else if (key === 'dangerouslySetInnerHTML') {
        if (val && val.__html != null) {
          dom.innerHTML = val.__html;
        }
      } else {
        if (val == null || val === false) {
          dom.removeAttribute(key);
        } else {
          dom.setAttribute(key, val === true ? '' : String(val));
        }
      }
    }
  }

  function patch(parentDom, newVNode, oldVNode, nextSibling, isParentSvg) {
    if (newVNode == null || typeof newVNode === 'boolean') {
      if (oldVNode) unmount(oldVNode, parentDom);
      return null;
    }

    if (typeof newVNode === 'string' || typeof newVNode === 'number') {
      newVNode = { type: '#text', text: String(newVNode), props: null, key: null };
    }

    if (newVNode.type === '#text') {
      if (oldVNode && oldVNode.type === '#text' && oldVNode._dom) {
        if (oldVNode._dom.nodeValue !== newVNode.text) {
          oldVNode._dom.nodeValue = newVNode.text;
        }
        newVNode._dom = oldVNode._dom;
        return oldVNode._dom;
      }
      if (oldVNode) unmount(oldVNode, parentDom);
      const textNode = document.createTextNode(newVNode.text);
      parentDom.insertBefore(textNode, nextSibling || null);
      newVNode._dom = textNode;
      return textNode;
    }

    if (typeof newVNode.type === 'function') {
      if (oldVNode && oldVNode.type !== newVNode.type) {
        unmount(oldVNode, parentDom);
        oldVNode = null;
      }
      const instance = (oldVNode && oldVNode.type === newVNode.type && oldVNode._instance)
        ? oldVNode._instance
        : { hooks: [] };
      newVNode._instance = instance;
      instance.vnode = newVNode;

      const prevComp = currentComponent;
      const prevHookIdx = currentHookIndex;
      currentComponent = instance;
      currentHookIndex = 0;

      let subTree = null;
      try {
        subTree = newVNode.type(newVNode.props || {});
      } catch (err) {
        console.error('[NagReact] Error rendering ' + (newVNode.type.name || 'Component') + ':', err);
      } finally {
        currentComponent = prevComp;
        currentHookIndex = prevHookIdx;
      }

      const oldSubTree = oldVNode ? oldVNode._subTree : null;
      const dom = patch(parentDom, subTree, oldSubTree, nextSibling, isParentSvg);
      newVNode._subTree = subTree;
      newVNode._dom = dom;
      return dom;
    }

    const isSvg = isParentSvg || SVG_TAGS.has(newVNode.type);
    let dom;

    if (oldVNode && oldVNode.type === newVNode.type && oldVNode._dom) {
      dom = oldVNode._dom;
      newVNode._dom = dom;
      updateProps(dom, newVNode.props, oldVNode.props, isSvg);
      reconcileChildren(dom, newVNode, oldVNode, isSvg);
    } else {
      if (oldVNode) unmount(oldVNode, parentDom);
      dom = isSvg
        ? document.createElementNS('http://www.w3.org/2000/svg', newVNode.type)
        : document.createElement(newVNode.type);
      newVNode._dom = dom;
      updateProps(dom, newVNode.props, null, isSvg);
      const flat = flattenChildren(newVNode.props && newVNode.props.children);
      newVNode._children = flat;
      for (let i = 0; i < flat.length; i++) {
        patch(dom, flat[i], null, null, isSvg);
      }
      parentDom.insertBefore(dom, nextSibling || null);
    }

    return dom;
  }

  function reconcileChildren(parentDom, newVNode, oldVNode, isSvg) {
    const newFlat = flattenChildren(newVNode.props && newVNode.props.children);
    newVNode._children = newFlat;
    const oldFlat = (oldVNode && oldVNode._children) ? oldVNode._children : [];

    const hasKeys = newFlat.some(c => c && c.key != null) || oldFlat.some(c => c && c.key != null);
    if (!hasKeys) {
      const max = Math.max(newFlat.length, oldFlat.length);
      for (let i = 0; i < max; i++) {
        const n = newFlat[i];
        const o = oldFlat[i];
        if (n && o) patch(parentDom, n, o, null, isSvg);
        else if (n) patch(parentDom, n, null, null, isSvg);
        else if (o) unmount(o, parentDom);
      }
      return;
    }

    const oldKeyMap = new Map();
    for (let i = 0; i < oldFlat.length; i++) {
      const o = oldFlat[i];
      const k = (o && o.key != null) ? o.key : '__idx_' + i;
      oldKeyMap.set(k, o);
    }

    for (let i = 0; i < newFlat.length; i++) {
      const n = newFlat[i];
      const k = (n && n.key != null) ? n.key : '__idx_' + i;
      const o = oldKeyMap.get(k);
      if (o) {
        oldKeyMap.delete(k);
        patch(parentDom, n, o, null, isSvg);
      } else {
        patch(parentDom, n, null, null, isSvg);
      }
    }

    for (const o of oldKeyMap.values()) {
      unmount(o, parentDom);
    }
  }

  function unmount(vnode, parentDom) {
    if (!vnode) return;
    if (vnode._instance) {
      for (const h of vnode._instance.hooks) {
        if (h && typeof h.cleanup === 'function') {
          try { h.cleanup(); } catch (err) { console.error('[NagReact] Unmount cleanup error:', err); }
        }
      }
    }
    if (vnode._subTree) {
      unmount(vnode._subTree, parentDom);
    }
    if (vnode._dom && parentDom && vnode._dom.parentNode === parentDom) {
      parentDom.removeChild(vnode._dom);
    }
    const flat = vnode._children || [];
    for (let i = 0; i < flat.length; i++) {
      unmount(flat[i], vnode._dom);
    }
  }

  function mount(element, container) {
    rootContainer = container;
    rootElement = element;
    renderRoot();
  }

  const Children = {
    map(children, fn) {
      const flat = flattenChildren(children);
      return flat.map(fn);
    },
    forEach(children, fn) {
      const flat = flattenChildren(children);
      flat.forEach(fn);
    },
    count(children) {
      return flattenChildren(children).length;
    },
    toArray(children) {
      return flattenChildren(children);
    },
  };

  return {
    createElement,
    jsx,
    jsxs,
    Fragment,
    useState,
    useRef,
    useMemo,
    useCallback,
    useEffect,
    Children,
    mount,
  };
})();

if (typeof window !== 'undefined') {
  window.React = NagReact;
  window.NagReactMount = NagReact.mount;
}
`;

// Build final self-contained JavaScript bundle
let bundleContent = `/**
 * NAG Official Candidate Mobile - Complete Self-Contained Web Asset Bundle
 * Build Timestamp: ${new Date().toISOString()}
 * Target: Android WebView file:///android_asset/www/index.html
 */
(function() {
  'use strict';

  ${runtimeShim}

  const __nag_modules = {};
  const __nag_installed_modules = {};

  function __nag_require(id) {
    if (__nag_installed_modules[id]) {
      return __nag_installed_modules[id].exports;
    }
    const moduleDef = __nag_modules[id];
    if (!moduleDef) {
      // Handle known built-in fallbacks
      if (id === 'react') return NagReact;
      if (id === 'react/jsx-runtime') return { jsx: NagReact.jsx, jsxs: NagReact.jsxs, Fragment: NagReact.Fragment };
      if (id === 'react-native') return { NativeModules: {}, TurboModuleRegistry: { get: () => null } };
      if (id === 'crypto') return {};
      if (id.startsWith('expo-')) return {};
      throw new Error('[NAG Bundle Loader] Module not found: ' + id);
    }

    const module = __nag_installed_modules[id] = { exports: {} };
    function localRequire(dep) {
      const resolved = moduleDef.deps[dep] || dep;
      return __nag_require(resolved);
    }

    moduleDef.factory(module, module.exports, localRequire);
    return module.exports;
  }
`;

for (const [id, def] of Object.entries(transpiledModules)) {
  const depsJson = JSON.stringify(def.deps);
  bundleContent += `
  __nag_modules[${JSON.stringify(id)}] = {
    deps: ${depsJson},
    factory: function(module, exports, require) {
${def.code}
    }
  };
`;
}

// Entrypoint invocation
bundleContent += `
  // Automatically execute the mobile web entry point
  try {
    __nag_require('@app/web-entry.tsx');
  } catch (err) {
    console.error('[NAG Candidate Mobile] Bootstrapping failed:', err);
  }
})();
`;

const bundlePath = path.join(ASSETS_DIR, 'app.bundle.js');
fs.writeFileSync(bundlePath, bundleContent, 'utf8');

const htmlContent = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover" />
  <meta name="theme-color" content="#1A365D" />
  <title>NAG Candidate Mobile</title>
  <style>
    * {
      box-sizing: border-box;
      -webkit-tap-highlight-color: transparent;
      user-select: text;
    }
    html, body {
      margin: 0;
      padding: 0;
      width: 100%;
      height: 100%;
      background-color: #EDF2F7;
      color: #1A202C;
      font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
      overflow-x: hidden;
    }
    #root {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
    }
  </style>
</head>
<body>
  <div id="root"></div>
  <script src="app.bundle.js"></script>
</body>
</html>
`;

const htmlPath = path.join(ASSETS_DIR, 'index.html');
fs.writeFileSync(htmlPath, htmlContent, 'utf8');

const bundleStat = fs.statSync(bundlePath);
const htmlStat = fs.statSync(htmlPath);

console.log('[NAG Assets Builder] Assets generated successfully:');
console.log(`  -> ${bundlePath} (${(bundleStat.size / 1024).toFixed(1)} KB)`);
console.log(`  -> ${htmlPath} (${htmlStat.size} bytes)`);
