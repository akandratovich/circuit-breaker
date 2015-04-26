package me.kondratovich;

interface State {
  <T> T invoke(Operation<T> delegate);

  void success();

  void fail();

  void enter();
}