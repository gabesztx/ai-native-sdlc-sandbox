// @ts-check
const eslint = require("@eslint/js");
const { defineConfig } = require("eslint/config");
const tseslint = require("typescript-eslint");
const angular = require("angular-eslint");

const FEATURES = ["auth", "forms", "reviews", "team"];

const BASE_PATHS = [
  { name: "rxjs/operators", message: "Import from 'rxjs'." },
  { name: "@angular/animations", message: "Use native CSS animations." },
  {
    name: "@angular/forms",
    importNames: [
      "ReactiveFormsModule",
      "FormGroup",
      "FormControl",
      "FormArray",
      "FormRecord",
      "FormBuilder",
      "NonNullableFormBuilder",
      "UntypedFormGroup",
      "UntypedFormControl",
      "UntypedFormArray",
      "UntypedFormBuilder",
    ],
    message: "Use Signal Forms from '@angular/forms/signals'.",
  },
  {
    name: "@angular/core",
    importNames: ["HostBinding", "HostListener"],
    message: "Use the host property of @Component / @Directive.",
  },
];

const BASE_PATTERNS = [
  { group: ["@angular/animations/*"], message: "Use native CSS animations." },
  {
    group: [
      "**/api/fn",
      "**/api/fn/*",
      "**/api/functions",
      "**/api/api",
      "**/api/request-builder",
      "**/api/strict-http-response",
    ],
    message: "Use the generated services through a feature or core service.",
  },
];

const COMPONENT_PATHS = [
  {
    name: "@angular/common/http",
    importNames: ["HttpClient"],
    message: "Components do not call HttpClient; use a feature service.",
  },
];

const COMPONENT_PATTERNS = [
  { group: ["**/api/services", "**/api/services/*"], message: "Components use a feature service, not a generated service." },
];

function restrictedImports({ paths = [], patterns = [] } = {}) {
  return { paths: [...BASE_PATHS, ...paths], patterns: [...BASE_PATTERNS, ...patterns] };
}

function otherFeaturesPattern(feature) {
  const others = FEATURES.filter((name) => name !== feature).join("|");
  return {
    regex: `^(\\./)?(\\.\\./)+(features/)?(${others})/`,
    message: "A feature does not import from another feature.",
  };
}

module.exports = defineConfig([
  {
    ignores: ["src/app/core/api/**"],
  },
  {
    files: ["**/*.ts"],
    extends: [
      eslint.configs.recommended,
      tseslint.configs.recommended,
      tseslint.configs.stylistic,
      angular.configs.tsRecommended,
    ],
    processor: angular.processInlineTemplates,
    rules: {
      "@angular-eslint/directive-selector": [
        "error",
        { type: "attribute", prefix: [], style: "camelCase" },
      ],
      "@angular-eslint/component-selector": [
        "error",
        { type: "element", prefix: [], style: "kebab-case" },
      ],
      "@angular-eslint/prefer-on-push-component-change-detection": "error",
      "@angular-eslint/component-max-inline-declarations": [
        "error",
        { template: 0, styles: 0, animations: 0 },
      ],
      "@typescript-eslint/no-explicit-any": "error",
      "@typescript-eslint/explicit-member-accessibility": [
        "error",
        { accessibility: "explicit", overrides: { constructors: "off" } },
      ],
      "no-restricted-imports": ["error", restrictedImports()],
      "no-restricted-syntax": [
        "error",
        {
          selector: "PropertyDefinition[accessibility='protected'], MethodDefinition[accessibility='protected']",
          message: "Use public or private, not protected.",
        },
        {
          selector: "PropertyDefinition[readonly=true]",
          message: "Do not use readonly on class members.",
        },
        {
          selector: "Decorator[expression.callee.name=/^(Input|Output)$/]",
          message: "Use input() / output() instead of @Input() / @Output().",
        },
        {
          selector: "Decorator[expression.callee.name=/^(HostBinding|HostListener)$/]",
          message: "Use the host property of @Component / @Directive.",
        },
        {
          selector: "MemberExpression[object.name='ChangeDetectionStrategy'][property.name='Eager']",
          message: "OnPush is required; do not use Eager change detection.",
        },
        {
          selector: "Property[key.name='standalone'][value.value=true]",
          message: "standalone: true is the default; do not write it.",
        },
      ],
    },
  },
  {
    files: ["src/app/**/*.component.ts"],
    rules: {
      "no-restricted-imports": ["error", restrictedImports({ paths: COMPONENT_PATHS, patterns: COMPONENT_PATTERNS })],
    },
  },
  ...FEATURES.flatMap((feature) => [
    {
      files: [`src/app/features/${feature}/**/*.ts`],
      ignores: ["**/*.component.ts"],
      rules: {
        "no-restricted-imports": ["error", restrictedImports({ patterns: [otherFeaturesPattern(feature)] })],
      },
    },
    {
      files: [`src/app/features/${feature}/**/*.component.ts`],
      rules: {
        "no-restricted-imports": [
          "error",
          restrictedImports({
            paths: COMPONENT_PATHS,
            patterns: [...COMPONENT_PATTERNS, otherFeaturesPattern(feature)],
          }),
        ],
      },
    },
  ]),
  {
    files: ["**/*.html"],
    extends: [
      angular.configs.templateRecommended,
      angular.configs.templateAccessibility,
    ],
    rules: {
      "@angular-eslint/template/prefer-control-flow": "error",
    },
  },
]);
