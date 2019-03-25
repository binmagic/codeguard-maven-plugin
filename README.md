# codeguard-maven-plugin
maven plugin for java byte code encrypt!

## 使用
#### 简单说明
　本插件实现了在package阶段对输出jar包进行指定文件的加密,加密后生成新的jar包.默认对packaging为pom的项目会自动跳过
#### 简单引入
```
<plugin>
		<groupId>com.github.binmagic</groupId>
		<artifactId>codeguard-maven-plugin</artifactId>
		<version>1.0-SNAPSHOT</version>
		<executions>
			<execution>
				<goals>
					<goal>guard</goal>
				</goals>
			</execution>
		</executions>
		<configuration>
			<injar>${project.build.finalName}.jar</injar>
			<outjar>${project.build.finalName}-pg.jar</outjar>
			<includes>
                <include>.*\.class</include>
            </includes>
            <excludes>
                <exclude>.*H\.class</exclude>
            </excludes>
		</configuration>
</plugin>
```
#### 配置说明
* injar 要进行加密jar包的名称
* outjar 加密后jar包的名称
* inputDirectory injar所在目录(默认为maven打包的输出目录)
* outputDirectory outjar输出目录(默认文maven打包的输出目录)(要求injar和outjar绝地路径不能相同)
* includes set类型,使用正则表达式进行路径匹配(路径:xx/yy/xx.xx)(未配置则默认匹配 .*\.class)
* excludes set类型,使用正则表达式进行路径匹配(排除的匹配成功一定不加密)
* removeTempOutDir boolean类型,表示是否删除加密过程中生成的临时文件夹
* skip boolean类型,表示是否跳过加密操作
* coverOriginal boolean类型,表示是否覆盖原始jar包