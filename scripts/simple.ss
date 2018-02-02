
if (command < 2) {
  println((command, "double", double(command)))
}
else if (command < 4) {
  println((command, "square", square(command)))
}
else {
  println((command, "sum", sum(doubleAsync(command - 4), squareAsync(4))))
}
