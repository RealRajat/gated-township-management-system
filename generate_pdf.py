import os
import subprocess
from markdown_it import MarkdownIt

def build_pdf():
    md_path = r"c:\Users\rkraj\OneDrive\Desktop\Gated Township Management System\Technical_Design_Document.md"
    html_path = r"c:\Users\rkraj\OneDrive\Desktop\Gated Township Management System\Technical_Design_Document.html"
    pdf_path = r"c:\Users\rkraj\OneDrive\Desktop\Gated Township Management System\Technical_Design_Document.pdf"
    edge_path = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"

    with open(md_path, "r", encoding="utf-8") as f:
        md_content = f.read()

    md = MarkdownIt("commonmark").enable("table")
    rendered_body = md.render(md_content)

    html_template = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>Technical Design Document - Gated Township Resident Management System</title>
<style>
  @page {{
    size: A4;
    margin: 18mm 16mm 18mm 16mm;
    @bottom-center {{
      content: counter(page);
      font-size: 9pt;
      color: #64748b;
    }}
  }}

  body {{
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
    color: #1e293b;
    background-color: #ffffff;
    line-height: 1.55;
    font-size: 10.5pt;
  }}

  h1 {{
    font-size: 20pt;
    font-weight: 800;
    color: #0f172a;
    border-bottom: 2px solid #0d9488;
    padding-bottom: 6px;
    margin-top: 0;
    margin-bottom: 12px;
  }}

  h2 {{
    font-size: 14pt;
    font-weight: 700;
    color: #0f172a;
    border-bottom: 1px solid #cbd5e1;
    padding-bottom: 4px;
    margin-top: 18pt;
    margin-bottom: 8pt;
    page-break-after: avoid;
  }}

  h3 {{
    font-size: 11.5pt;
    font-weight: 600;
    color: #0d9488;
    margin-top: 14pt;
    margin-bottom: 6pt;
    page-break-after: avoid;
  }}

  p, li {{
    color: #334155;
    margin-bottom: 6pt;
  }}

  strong {{
    color: #0f172a;
  }}

  table {{
    width: 100%;
    border-collapse: collapse;
    margin: 10pt 0;
    font-size: 9pt;
    page-break-inside: avoid;
  }}

  th, td {{
    border: 1px solid #cbd5e1;
    padding: 6pt 8pt;
    text-align: left;
  }}

  th {{
    background-color: #f1f5f9;
    color: #0f172a;
    font-weight: 700;
  }}

  tr:nth-child(even) {{
    background-color: #f8fafc;
  }}

  pre {{
    background-color: #0f172a;
    color: #f8fafc;
    padding: 10pt;
    border-radius: 6px;
    font-family: Consolas, "Courier New", monospace;
    font-size: 8.5pt;
    overflow-x: auto;
    margin: 10pt 0;
    page-break-inside: avoid;
  }}

  code {{
    font-family: Consolas, "Courier New", monospace;
    font-size: 9pt;
    background-color: #f1f5f9;
    color: #0d9488;
    padding: 1pt 3pt;
    border-radius: 4px;
  }}

  pre code {{
    background-color: transparent;
    color: #f8fafc;
    padding: 0;
  }}

  blockquote {{
    border-left: 4px solid #0d9488;
    background-color: #f0fdfa;
    color: #115e59;
    margin: 10pt 0;
    padding: 8pt 12pt;
    font-size: 9.5pt;
    border-radius: 0 6px 6px 0;
  }}

  hr {{
    border: none;
    border-top: 1px solid #e2e8f0;
    margin: 14pt 0;
  }}
</style>
</head>
<body>
{rendered_body}
</body>
</html>"""

    with open(html_path, "w", encoding="utf-8") as f:
        f.write(html_template)

    html_uri = "file:///" + html_path.replace("\\", "/")

    print(f"Generated styled HTML: {html_path}")
    print("Converting HTML to PDF via Microsoft Edge headless...")

    cmd = [
        edge_path,
        "--headless",
        "--disable-gpu",
        "--no-pdf-header-footer",
        f"--print-to-pdf={pdf_path}",
        html_uri
    ]

    subprocess.run(cmd, check=True)

    if os.path.exists(pdf_path):
        size_kb = os.path.getsize(pdf_path) / 1024
        print(f"SUCCESS: {pdf_path} created ({size_kb:.1f} KB)")
    else:
        print("ERROR: PDF output not found.")

if __name__ == "__main__":
    build_pdf()
