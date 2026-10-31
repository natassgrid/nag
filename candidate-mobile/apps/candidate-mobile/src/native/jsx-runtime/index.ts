import React from 'react';
import { View, Text, TouchableOpacity, TextInput } from 'react-native';

const wrapChildren = (children: any): any => {
  if (typeof children === 'string' || typeof children === 'number') {
    return React.createElement(Text, null, children);
  }
  if (Array.isArray(children)) {
    return children.map(wrapChildren);
  }
  if (React.isValidElement(children)) {
    const elProps = (children as any).props;
    if (elProps && elProps.children !== undefined) {
      return React.cloneElement(children, {}, wrapChildren(elProps.children));
    }
  }
  return children;
};

const MappedContainer = (Component: any) => {
  return function WrappedComponent(props: any) {
    const newProps = { ...props };
    if (newProps.children !== undefined) {
      newProps.children = wrapChildren(newProps.children);
    }
    return React.createElement(Component, newProps);
  };
};

const MappedView = MappedContainer(View);
const MappedTouchable = MappedContainer(TouchableOpacity);

export function jsx(type: any, props: any, key: any) {
  if (typeof type === 'string') {
    switch (type) {
      case 'div':
      case 'header':
      case 'footer':
      case 'section':
      case 'nav':
      case 'main':
      case 'aside':
      case 'article':
      case 'ul':
      case 'li':
      case 'form':
        type = MappedView;
        break;
      case 'span':
      case 'p':
      case 'h1':
      case 'h2':
      case 'h3':
      case 'h4':
      case 'h5':
      case 'h6':
      case 'label':
      case 'strong':
      case 'em':
      case 'small':
      case 'b':
      case 'i':
        type = Text;
        break;
      case 'button':
      case 'a':
        type = MappedTouchable;
        break;
      case 'input':
        type = TextInput;
        break;
    }
  }
  return React.createElement(type, key !== undefined ? { ...props, key } : props);
}

export const jsxs = jsx;
export const Fragment = React.Fragment;
