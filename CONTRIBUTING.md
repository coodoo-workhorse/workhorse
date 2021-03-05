# Contributing

When contributing to this repository, please first discuss the change you wish to make via issue,
email, or any other method with the owners of this repository before making a change. 


## Pull Request Process

1. Ensure the pull request only contains the wanted changes and files. Feel free to add IDE specific settings to git ignore.
2. Ensure keep the [Coding Conventions](#coding-conventions) and fullfill the [Definition of Done](#definition-of-done).
3. Consider the effects your change has on depended sub projects.
4. Like the Boy Scouts: Always leave the code cleaner than you found it! 
5. We don't need no bullshit Code of Conduct, feel free to offend whatever and whomever you want!


## Coding Conventions

- Simplicity first, *the code must be easy to read and understand*
- All is in English
- Avoid shotcuts in names
- When in doubt name or do it like others did
- Leave short comments on complicated code


## Definition of Done

- There is an entry in the [changelog](./CHANGELOG.md)
- The [documentation](./README.md) is updated
- There is JavaDoc on public classes and methods
- There are sufficient JUnit tests
- There are test cases in the `worhorse-test` project
- There are well explained examples in the available `worhorse-example-*` projects


## Formatting rules
We use the Ecplise Formatter to format our code. The configuration file that
instructs the formatter on how to format the code is:
```sh
src/main/resources/coodooJavaStyle.xml
```
You can [consult the official documentation of Eclipse][1] to learn how to use
this configuration as your default formatting rules. Probably it makes sense to
[format code automagically on each save][2].
To check for correct formatting in our GitLab Pipelines, we use
[spotless-maven-plugin][spotless]. If you do not use Eclipse, you can leverage
the plugin to format the entire codebase with the following command:
```sh
mvn spotless:apply
```
or you can format individual files with the following command:
```sh
mvn spotless:apply -DspotlessFiles=my/file/pattern.java,more/generic/.*-pattern.java
```
[1]: https://help.eclipse.org/neon/index.jsp?topic=%2Forg.eclipse.jdt.doc.user%2Freference%2Fpreferences%2Fjava%2Fcodestyle%2Fref-preferences-formatter.htm
[2]: http://www.eclipseonetips.com/2009/12/13/automatically-format-and-cleanup-code-every-time-you-save/
[spotless]: https://github.com/diffplug/spotless
[spotless-specific]: https://github.com/diffplug/spotless/blob/master/plugin-maven/README.md#can-i-apply-spotless-to-specific-files
