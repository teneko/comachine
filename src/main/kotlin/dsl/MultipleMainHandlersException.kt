package de.halfbit.comachine.dsl

class MultipleMainHandlersException(blockName: String) :
    IllegalArgumentException("Main $blockName handler is already declared. Only one main handler is allowed.")