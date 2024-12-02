module github.com/invisv-privacy/bindings

go 1.22.2

require github.com/google/gopacket v1.1.19

require github.com/invisv-privacy/pseudotcp v0.0.0

require (
	golang.org/x/mobile v0.0.0-20241108191957-fa514ef75a0f // indirect
	golang.org/x/mod v0.22.0 // indirect
	golang.org/x/sync v0.9.0 // indirect
	golang.org/x/tools v0.27.0 // indirect
)

replace github.com/invisv-privacy/pseudotcp => ../../../../pseudotcp
