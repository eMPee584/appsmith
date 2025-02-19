import React from "react";
import type { ComponentProps } from "react";

export const CheckIcon = (props: ComponentProps<"svg">) => {
  return (
    <svg
      className="icon icon-tabler icon-tabler-check"
      fill="none"
      stroke="currentColor"
      strokeLinecap="round"
      strokeLinejoin="round"
      strokeWidth="2"
      viewBox="0 0 24 24"
      xmlns="http://www.w3.org/2000/svg"
      {...props}
    >
      <path d="M0 0h24v24H0z" fill="none" stroke="none" />
      <path d="M5 12l5 5l10 -10" />
    </svg>
  );
};
