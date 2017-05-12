#!/bin/bash
echo "If prompted for password, just hit enter (it's blank)"
scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no lib/native/lib/*.so admin@roborio-1736-frc.local:/usr/local/frc/lib/
read -p "Press enter to exit..."