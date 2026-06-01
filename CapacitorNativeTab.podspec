require 'json'
package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name = 'CapacitorNativeTab'
  s.version = package['version']
  s.summary = 'Capacitor plugin for native tab bars'
  s.license = { type: 'MIT', file: 'LICENSE' }
  s.homepage = 'https://github.com/ACM-VIT/capacitor-native-tab'
  s.authors = { 'ACM-VIT' => 'opensource@acmvit.in' }
  s.source = { git: 'https://github.com/ACM-VIT/capacitor-native-tab.git', tag: "v#{s.version}" }
  s.platform = :ios, '15.0'
  s.source_files = 'ios/Sources/**/*.swift'
  s.requires_arc = true
  s.swift_version = '5.9'
  s.dependency 'Capacitor', '~> 8.0'
end
