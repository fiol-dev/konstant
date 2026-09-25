# Module konstant-annotations

Annotations you put on a config class. `@ConfigSpec` marks the class for the KSP processor,
which generates its loader. The other annotations tune single fields: `@Key` renames the key,
`@Secret` masks the value in reports and errors, `@Convert` plugs in a custom
`ValueConverter`, and `@Range`, `@Size`, `@NotBlank` and `@Pattern` validate values.

# Package io.github.fiol_dev.konstant.annotations

Annotations read by the Konstant KSP processor.
