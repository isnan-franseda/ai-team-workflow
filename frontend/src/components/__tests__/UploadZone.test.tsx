import { render, screen, fireEvent } from "@testing-library/react";
import { UploadZone } from "../UploadZone";

describe("UploadZone", () => {
  const mockOnFilesSelected = vi.fn();
  const mockOnDocTypesChanged = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should render drag and drop zone", () => {
    render(
      <UploadZone
        onFilesSelected={mockOnFilesSelected}
        onDocTypesChanged={mockOnDocTypesChanged}
        files={[]}
        docTypes={[]}
      />
    );

    expect(screen.getByText("Drag and drop files here")).toBeInTheDocument();
    expect(screen.getByText("atau klik untuk memilih file")).toBeInTheDocument();
    expect(screen.getByText("PDF, DOCX, atau TXT")).toBeInTheDocument();
  });

  it("should have file input with correct accept attribute", () => {
    render(
      <UploadZone
        onFilesSelected={mockOnFilesSelected}
        onDocTypesChanged={mockOnDocTypesChanged}
        files={[]}
        docTypes={[]}
      />
    );

    const fileInput = screen.getByLabelText("Upload File Drop Zone");
    expect(fileInput).toHaveAttribute("accept", ".pdf,.docx,.txt");
    expect(fileInput).toHaveAttribute("multiple");
  });

  it("should call onFilesSelected when file input changes", () => {
    render(
      <UploadZone
        onFilesSelected={mockOnFilesSelected}
        onDocTypesChanged={mockOnDocTypesChanged}
        files={[]}
        docTypes={[]}
      />
    );

    const fileInput = screen.getByLabelText("Upload File Drop Zone");
    const file = new File(["test content"], "test.pdf", { type: "application/pdf" });
    fireEvent.change(fileInput, { target: { files: [file] } });

    expect(mockOnFilesSelected).toHaveBeenCalled();
    expect(mockOnDocTypesChanged).toHaveBeenCalled();
  });

  it("should show selected files list when files are present", () => {
    const testFiles = [new File(["test"], "test.pdf", { type: "application/pdf" })];
    render(
      <UploadZone
        onFilesSelected={mockOnFilesSelected}
        onDocTypesChanged={mockOnDocTypesChanged}
        files={testFiles}
        docTypes={["FAQ"]}
      />
    );

    expect(screen.getByText("File Terpilih:")).toBeInTheDocument();
    expect(screen.getByText("test.pdf")).toBeInTheDocument();
  });

  it("should render doc type selector for each file", () => {
    const testFiles = [
      new File(["test1"], "test1.pdf", { type: "application/pdf" }),
      new File(["test2"], "test2.docx", { type: "application/vnd.openxmlformats-officedocument.wordprocessingml.document" }),
    ];
    render(
      <UploadZone
        onFilesSelected={mockOnFilesSelected}
        onDocTypesChanged={mockOnDocTypesChanged}
        files={testFiles}
        docTypes={["FAQ", "TOS"]}
      />
    );

    const selectors = screen.getAllByRole("combobox");
    expect(selectors).toHaveLength(2);
  });

  it("should update doc type when selector changes", () => {
    const testFiles = [new File(["test"], "test.pdf", { type: "application/pdf" })];
    render(
      <UploadZone
        onFilesSelected={mockOnFilesSelected}
        onDocTypesChanged={mockOnDocTypesChanged}
        files={testFiles}
        docTypes={["FAQ"]}
      />
    );

    const selector = screen.getByLabelText("Doc Type for test.pdf");
    fireEvent.change(selector, { target: { value: "TOS" } });

    expect(mockOnDocTypesChanged).toHaveBeenCalledWith(["TOS"]);
  });

  it("should be disabled when disabled prop is true", () => {
    const testFiles = [new File(["test"], "test.pdf", { type: "application/pdf" })];
    render(
      <UploadZone
        onFilesSelected={mockOnFilesSelected}
        onDocTypesChanged={mockOnDocTypesChanged}
        files={testFiles}
        docTypes={["FAQ"]}
        disabled={true}
      />
    );

    const fileInput = screen.getByLabelText("Upload File Drop Zone");
    expect(fileInput).toBeDisabled();
  });

  it("should show drag active state styling", () => {
    const { container } = render(
      <UploadZone
        onFilesSelected={mockOnFilesSelected}
        onDocTypesChanged={mockOnDocTypesChanged}
        files={[]}
        docTypes={[]}
      />
    );

    const dropZone = container.querySelector(".border-dashed");
    expect(dropZone).toBeInTheDocument();
  });
});
